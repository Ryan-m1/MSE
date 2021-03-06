package cn.wormholestack.mse.facadeImpl.impl;

import cn.wormholestack.mse.business.gateway.ProxyService;
import cn.wormholestack.mse.common.constant.ErrorMsgConstant;
import cn.wormholestack.mse.common.enums.GatewayServiceEnum;
import cn.wormholestack.mse.common.exception.*;
import cn.wormholestack.mse.common.model.base.BaseReq;
import cn.wormholestack.mse.common.model.base.BaseRes;
import cn.wormholestack.mse.common.model.base.ResponseContext;
import cn.wormholestack.mse.facadeImpl.GatewayContext;
import cn.wormholestack.mse.facadeImpl.GatewayServiceEntrance;
import cn.wormholestack.mse.facadeImpl.GatewayServiceProvider;
import cn.wormholestack.mse.facadeImpl.Interceptor.Interceptor;
import cn.wormholestack.mse.facadeImpl.converter.Converter;
import cn.wormholestack.mse.facadeImpl.validator.Validator;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

/**
 * @description： 网关入口默认实现
 * @Author MRyan
 * @Date 2021/11/13 15:54
 * @Version 1.0
 */
@Service
public class DefaultGatewayServiceEntrance<Req extends BaseReq, Res extends BaseRes, InVO, OutVO>
        implements GatewayServiceEntrance<Req, Res, InVO, OutVO> {

    private static Logger logger = LoggerFactory.getLogger(DefaultGatewayServiceEntrance.class);

    /**
     * 服务网关提供商
     */
    @Resource
    private GatewayServiceProvider<Req, Res, InVO, OutVO> provider;


    @Override
    public Res invoke(GatewayServiceEnum gatewayMethod, Req req, Res res) {
        GatewayContext<InVO, OutVO> context = new GatewayContext<>(gatewayMethod);
        context.getStopWatch().start();
        Res result = null;
        logger.info("MSEGateway调用入参={}", JSON.toJSON(req));
        try {
            //1.请求参数校验
            doValidation(req, context);
            //2.数据转换
            doPreConverter(req, context);
            //3.拦截器，前置处理器
            context.setRequest(doInterceptor(context));
            //4.调用服务
            doCallService(context);
            //5.数据转换
            result = doPostConverter(context);
            return result;
        } catch (InterceptException e) {
            result = getResFromException(context, res, new InterceptException(ErrorMsgConstant.INTERCEPT_ERROR_MSG, e));
            return result;
        } catch (ValidateException e) {
            result = getResFromException(context, res, new ValidateException(ErrorMsgConstant.VALIDATE_ERROR_MSG, e));
            return result;
        } catch (ConverterException e) {
            result = getResFromException(context, res, new ConverterException(ErrorMsgConstant.CONVERTER_ERROR_MSG, e));
            return result;
        } catch (ServiceException e) {
            result = getResFromException(context, res, new ServiceException(ErrorMsgConstant.SERVICE_ERROR_MSG, e));
            return result;
        } catch (Exception e) {
            result = getResFromException(context, res, new MSEException(ErrorMsgConstant.MSE_ERROR_MSG, e));
            return result;
        } finally {
            logger.info("Gateway 调用出参={}", JSON.toJSONString(result));
            if (context.getStopWatch().isRunning()) {
                context.getStopWatch().stop();
            }
        }
    }

    /**
     * 拦截器
     *
     * @param context
     */
    private InVO doInterceptor(GatewayContext<InVO, OutVO> context) throws InterceptException {
        Interceptor interceptor = provider.getInterceptor(context.getMethod().getService());
        if (interceptor == null) {
            throw new IllegalArgumentException("[Interceptor is invalid] cannot found Interceptor for interceptor: " + interceptor);
        }
        return interceptor.intercept(context);
    }

    /**
     * 前置出参转换
     *
     * @param context
     * @return
     */
    private Res doPostConverter(GatewayContext<InVO, OutVO> context) throws ConverterException {
        Converter<Req, Res, InVO, OutVO> converter = provider.getConverter(context.getMethod().getService());
        if (converter == null) {
            throw new IllegalArgumentException("[Converter is invalid] cannot found Converter for converter: " + context.getMethod().getService());
        }
        startStopWatch(context.getStopWatch(), "postConverter");
        Res res = converter.voToResponse(context.getResponse());
        context.getStopWatch().stop();
        return res;
    }

    /**
     * 前置入参转换
     *
     * @param req
     * @param context
     * @return
     */
    private InVO doPreConverter(Req req, GatewayContext<InVO, OutVO> context) throws ConverterException {
        Converter<Req, Res, InVO, OutVO> converter = provider.getConverter(context.getMethod().getService());
        if (converter == null) {
            throw new IllegalArgumentException("[Converter is invalid] cannot found Converter for converter: " + context.getMethod().getService());
        }
        startStopWatch(context.getStopWatch(), "preConverter");
        InVO inVO = converter.requestToVO(req);
        context.setRequest(inVO);
        context.getStopWatch().stop();
        return inVO;
    }

    private void doValidation(Req req, GatewayContext<InVO, OutVO> context) throws ValidateException {
        Validator<Req> validator = provider.getValidator(context.getMethod().getService());
        if (validator == null) {
            throw new IllegalArgumentException("[Validator is invalid] cannot found Validator for validator: " + context.getMethod().getService());
        }
        startStopWatch(context.getStopWatch(), "validate");
        validator.validate(req);
        context.getStopWatch().stop();
    }

    /**
     * 调用服务
     *
     * @param context
     * @return
     */
    private GatewayContext<InVO, OutVO> doCallService(GatewayContext<InVO, OutVO> context) throws ServiceException {
        ProxyService<InVO, OutVO> service = provider.getProxyService(context.getMethod().getService());
        if (service == null) {
            throw new IllegalArgumentException("[Service is invalid] cannot found  service: " + context.getMethod().getService());
        }
        startStopWatch(context.getStopWatch(), "service");
        ResponseContext<OutVO> responseContext = service.invoke(context.getRequest());
        context.setResponse(responseContext);
        context.getStopWatch().stop();
        return context;
    }


    private Res getResFromException(GatewayContext<InVO, OutVO> context, Res res, MSEException e) {
        logger.warn(e.getDetailMessage() + " msg={}", e.getMessage(), e);
        res.setSuccess(false);
        res.setMessage(e.getDetailMessage());
        return res;
    }


    /**
     * 启动计时器
     *
     * @param stopWatch
     * @param name
     */
    private void startStopWatch(StopWatch stopWatch, String name) {
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        }
        stopWatch.start(name);
    }
}
