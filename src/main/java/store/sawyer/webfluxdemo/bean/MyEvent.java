package store.sawyer.webfluxdemo.bean;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @ClassName MyEvent
 * @Author Sawyer Yong
 * @Date 2020-01-16 14:55
 * @Description 说点什么吧~
 */
@Data
@Document("event")
public class MyEvent {

    @Id
    private Long id;

    private String message;
}
