package net.pkhapps.idispatch.server.control;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.ScheduledTaskHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@code @Async} and {@code @Scheduled} annotations on the dispatchers are in effect. Without
 * {@code @EnableAsync} and {@code @EnableScheduling}, Spring silently ignores them.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class AsyncAndSchedulingTest {

    @Autowired
    RunboardDispatcher runboardDispatcher;
    @Autowired
    ScheduledTaskHolder scheduledTaskHolder;

    @Test
    void runboardDispatcher_isProxiedForAsyncExecution() {
        assertThat(AopUtils.isAopProxy(runboardDispatcher)).isTrue();
    }

    @Test
    void runboardDispatcherCleanUp_isScheduled() {
        assertThat(scheduledTaskHolder.getScheduledTasks())
                .extracting(task -> task.getTask().getRunnable().toString())
                .anyMatch(name -> name.contains("RunboardDispatcherBean.cleanUp"));
    }
}
