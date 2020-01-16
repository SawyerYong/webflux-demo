package store.sawyer.webfluxdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @ClassName TestController
 * @Author Sawyer Yong
 * @Date 2020-01-16 10:38
 * @Description 说点什么吧~
 */
@RestController
public class TestController {
    /**
     *  在传统controller中使用
     * @return
     */
    @GetMapping("hello")
    public Mono<String> helloWebFlux(){
        return Mono.just("hello spring webFlux");
    }
}
