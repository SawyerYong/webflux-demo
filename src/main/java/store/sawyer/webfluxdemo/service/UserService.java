package store.sawyer.webfluxdemo.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import store.sawyer.webfluxdemo.bean.User;

/**
 * @ClassName UserService
 * @Author Sawyer Yong
 * @Date 2020-01-16 14:10
 * @Description 说点什么吧~
 */
public interface UserService {
    /**
     * 保存用户
     * @param user
     * @return
     */
    Mono<User> save(User user);

    /**
     * 删除用户
     * @param username
     * @return
     */
    Mono<Long> deleteByUserNmae(String username);

    /**
     * 根据用户名查询用户
     * @param username
     * @return
     */
    Mono<User> getByUserName(String username);

    /**
     * 获取所有用户
     * @return
     */
    Flux<User> findAll();

}
