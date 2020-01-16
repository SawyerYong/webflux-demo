package store.sawyer.webfluxdemo.bean;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.Date;

/**
 * @ClassName User
 * @Author Sawyer Yong
 * @Date 2020-01-16 11:36
 * @Description 说点什么吧~
 */
@Data
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String phone;
    private String email;
    private String name;
    private Date birthday;
}
