package store.sawyer.webfluxdemo.flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import store.sawyer.webfluxdemo.bean.User;
import store.sawyer.webfluxdemo.service.UserService;

import java.time.Duration;

/**
 * @ClassName UserController
 * @Author Sawyer Yong
 * @Date 2020-01-16 14:25
 * @Description UserController
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public Mono<User> save(@RequestBody  User user){
        return userService.save(user);
    }

    @DeleteMapping("/{username}")
    public Mono<Long> deleteByUsername(@PathVariable String username){
        return userService.deleteByUserNmae(username);
    }

    @GetMapping("/{username}")
    public Mono<User> findByUserName(@PathVariable String username){
        return userService.getByUserName(username);
    }

    @GetMapping("")
    public Flux<User> findAll(){
        return userService.findAll();
    }


    /**
     * 延迟推送 每两秒推送一个结果
     * @return
     */
    @GetMapping(value = "/sse",produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<User> findAllSSE(){
        return userService.findAll().delayElements(Duration.ofSeconds(2));
    }




}
