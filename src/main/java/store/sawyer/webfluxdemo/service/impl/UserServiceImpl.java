package store.sawyer.webfluxdemo.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import store.sawyer.webfluxdemo.bean.User;
import store.sawyer.webfluxdemo.dao.UserDao;
import store.sawyer.webfluxdemo.service.UserService;

/**
 * @ClassName UserServiceImpl
 * @Author Sawyer Yong
 * @Date 2020-01-16 14:10
 * @Description 说点什么吧~
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;


    /**
     * 保存或更新。
     * 如果传入的user没有id属性，由于username是unique的，在重复的情况下有可能报错，
     * 这时找到以保存的user记录用传入的user更新它
     * @param user
     * @return
     */
    @Override
    public Mono<User> save(User user) {
        return userDao.save(user)
                .onErrorResume(e ->userDao.findByUsername(user.getUsername()))
                .doOnError(e->log.info("更新了user信息,username="+user.getUsername()))
                .flatMap(user1 -> {
                    user.setId(user1.getId());
                    return userDao.save(user1);
                });
    }

    @Override
    public Mono<Long> deleteByUserNmae(String username) {
        return userDao.deleteByUsername(username);
    }

    @Override
    public Mono<User> getByUserName(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public Flux<User> findAll() {
        System.out.println("查询了一次");
        return userDao.findAll();
    }
}
