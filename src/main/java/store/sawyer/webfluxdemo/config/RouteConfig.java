package store.sawyer.webfluxdemo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.*;
import store.sawyer.webfluxdemo.flux.TimeHandler;

/**
 * @ClassName RouteConfig
 * @Author Sawyer Yong
 * @Date 2020-01-16 11:08
 * @Description 说点什么吧~
 */
@Configuration
public class RouteConfig {
    @Autowired
    private TimeHandler timeHandler;

    @Bean
    public RouterFunction<ServerResponse> timeRouter(){
        return RouterFunctions.route(
                RequestPredicates.GET("/getTime"),serverRequest -> timeHandler.getTime(serverRequest))
                .andRoute(RequestPredicates.GET("/getTime2"),timeHandler::getTime)
                .andRoute(RequestPredicates.GET("/getTimeSSE"),timeHandler::getTimeSSE);

    }

}
