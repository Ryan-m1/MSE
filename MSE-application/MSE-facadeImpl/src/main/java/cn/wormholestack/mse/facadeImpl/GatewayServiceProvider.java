package cn.wormholestack.mse.facadeImpl;

import cn.wormholestack.mse.business.gateway.ProxyService;
import cn.wormholestack.mse.common.model.base.BaseReq;
import cn.wormholestack.mse.common.model.base.BaseRes;
import cn.wormholestack.mse.facadeImpl.Interceptor.Interceptor;
import cn.wormholestack.mse.facadeImpl.converter.Converter;
import cn.wormholestack.mse.facadeImpl.validator.Validator;

/**
 * @description： 网关服务提供者
 * @Author MRyan
 * @Date 2021/11/13 16:06
 * @Version 1.0
 */
public interface GatewayServiceProvider<Req extends BaseReq, Res extends BaseRes, InVO, OutVO> {

    /**
     * 获取网关服务代理服务接口
     *
     * @param ServiceName
     * @return
     */
    ProxyService<InVO, OutVO> getProxyService(String ServiceName);

    /**
     * 获取网关服务数据转换器
     *
     * @param serviceName
     * @return
     */
    Converter<Req, Res, InVO, OutVO> getConverter(String serviceName);

    /**
     * 网关服务校验器
     *
     * @param serviceName
     * @return
     */
    Validator<Req> getValidator(String serviceName);

    /**
     * 拦截器
     *
     * @param serviceName
     * @return
     */
    Interceptor getInterceptor(String serviceName);
}
