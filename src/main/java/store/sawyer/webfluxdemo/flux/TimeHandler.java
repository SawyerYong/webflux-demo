package store.sawyer.webfluxdemo.flux;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @ClassName TimeHandler
 * @Author Sawyer Yong
 * @Date 2020-01-16 11:03
 * @Description  相当于mvc里面的controller执行主体
 */
@Component
public class TimeHandler {

    /**
     * 获取时间接口
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getTime(ServerRequest serverRequest){
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN)
                .body(Mono.just("Now Time is "+ LocalDateTime.now()),String.class);
    }

    /**
     * SSE推送接口
     *
     * 使用Flux.interval定时生成数据流
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> getTimeSSE(ServerRequest serverRequest){
        return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.interval(Duration.ofSeconds(1))
                        .map(l->LocalDateTime.now().toString()),String.class);
    }
}
