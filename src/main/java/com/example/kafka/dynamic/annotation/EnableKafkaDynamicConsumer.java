import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(KafkaDynamicConsumerAutoConfiguration.class)
public @interface EnableKafkaDynamicConsumer {
} 