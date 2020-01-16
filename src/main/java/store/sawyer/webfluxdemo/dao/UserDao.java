package store.sawyer.webfluxdemo.dao;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import store.sawyer.webfluxdemo.bean.User;

/**
 * @ClassName UserDao
 * @Author Sawyer Yong
 * @Date 2020-01-16 14:09
 * @Description User持久层
 */
public interface UserDao extends ReactiveCrudRepository<User,String> {

    /**
     * 根据 username 查询用户信息
     * @param username
     * @return
     */
    Mono<User> findByUsername(String username);


    /**
     *  根据username删除
     * @param username
     * @return
     */
    Mono<Long> deleteByUsername(String username);

}
