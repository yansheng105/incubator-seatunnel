package io.github.interestinglab.waterdrop.flink.util;

import com.typesafe.config.waterdrop.Config;
import io.github.interestinglab.waterdrop.plugin.CheckResult;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author mr_xiong
 * @date 2019-08-24 15:55
 * @description
 */
public class EnvironmentUtil {

    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentUtil.class);

    public static void setRestartStrategy(Config config, Object environment) {
        try {
            if (config.hasPath(ConfigKeyName.RESTART_STRATEGY)) {
                String restartStrategy = config.getString(ConfigKeyName.RESTART_STRATEGY);
                Method method = environment.getClass().getMethod("setRestartStrategy");
                switch (restartStrategy.toLowerCase()) {
                    case "no":
                        method.invoke(environment, RestartStrategies.noRestart());
                        break;
                    case "fixed-delay":
                        int attempts = config.getInt(ConfigKeyName.RESTART_ATTEMPTS);
                        long delay = config.getLong(ConfigKeyName.RESTART_DELAY_BETWEEN_ATTEMPTS);
                        method.invoke(environment,RestartStrategies.fixedDelayRestart(attempts, delay));
                        break;
                    case "failure-rate":
                        long failureInterval = config.getLong(ConfigKeyName.RESTART_FAILURE_INTERVAL);
                        int rate = config.getInt(ConfigKeyName.RESTART_FAILURE_RATE);
                        long delayInterval = config.getLong(ConfigKeyName.RESTART_DELAY_INTERVAL);
                        method.invoke(environment,RestartStrategies.failureRateRestart(rate,
                                Time.of(failureInterval, TimeUnit.MILLISECONDS),
                                Time.of(delayInterval, TimeUnit.MILLISECONDS)));
                        break;
                    default:
                        LOG.warn("set restart.strategy failed, unknown restart.strategy [{}],only support no,fixed-delay,failure-rate", restartStrategy);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CheckResult checkRestartStrategy(Config config){
        if (config.hasPath(ConfigKeyName.RESTART_STRATEGY)) {
            String restartStrategy = config.getString(ConfigKeyName.RESTART_STRATEGY);
            switch (restartStrategy.toLowerCase()) {
                case "fixed-delay":
                    if (!(config.hasPath(ConfigKeyName.RESTART_ATTEMPTS)
                            && config.hasPath(ConfigKeyName.RESTART_DELAY_BETWEEN_ATTEMPTS))) {
                        return new CheckResult(false, String.format("fixed-delay restart strategy must set [%s],[%s]"
                                , ConfigKeyName.RESTART_ATTEMPTS, ConfigKeyName.RESTART_DELAY_BETWEEN_ATTEMPTS));
                    }
                    break;
                case "failure-rate":
                    if (!(config.hasPath(ConfigKeyName.RESTART_FAILURE_INTERVAL)
                            && config.hasPath(ConfigKeyName.RESTART_FAILURE_RATE)
                            && config.hasPath(ConfigKeyName.RESTART_DELAY_INTERVAL))) {
                        return new CheckResult(false, String.format("failure-rate restart strategy must set [%s],[%s],[%s]"
                                , ConfigKeyName.RESTART_FAILURE_INTERVAL, ConfigKeyName.RESTART_FAILURE_RATE, ConfigKeyName.RESTART_DELAY_INTERVAL));
                    }
                    break;
                default:
                    return new CheckResult(true, "");
            }
        }
        return new CheckResult(true,"");
    }
}