package com.baidu.filter;

import com.baidu.config.JwtConfig;
import com.baidu.shop.utils.CookieUtils;
import com.baidu.shop.utils.JwtUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName LoginFilter
 * @Description: TODO
 * @Author hexiangshen
 * @Date 2021/3/12
 * @Version V1.0
 **/
@Component
public class LoginFilter extends ZuulFilter {

    @Autowired
    private JwtConfig jwtConfig;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 5;
    }

    @Override
    public boolean shouldFilter() {

        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        String requestURI = request.getRequestURI();   

        return !jwtConfig.getExcludePath().contains(requestURI);
    }

    @Override
    public Object run() throws ZuulException {

        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();

        String token = CookieUtils.getCookieValue(request, jwtConfig.getCookieName(),true);

        try {
            JwtUtils.getInfoFromToken(token, jwtConfig.getPublicKey());

        } catch (Exception e) {
            e.printStackTrace();
            currentContext.setSendZuulResponse(false);
            currentContext.setResponseStatusCode(403);
        }

        return null;
    }
}