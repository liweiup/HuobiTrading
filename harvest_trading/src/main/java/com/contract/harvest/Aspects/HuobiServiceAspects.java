package com.contract.harvest.Aspects;


import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.ExceptionHandler;

@Aspect
public class HuobiServiceAspects {

    private static final Logger logger = LoggerFactory.getLogger(HuobiServiceAspects.class);

    @Pointcut("execution(public void com.contract.harvest.service.*.*(..))")
    public void pointCut(){};

    @Pointcut(
//            "execution(* com.contract.harvest.entity.*.*(..)) " +
            "execution(* com.contract.harvest.service.*.*(..)) " +
            "")
    public void pointCutVP(){};

    private static final long ONE_MINUTE = 20000L;

    @AfterThrowing(value="pointCut()",throwing="exception")
    @ExceptionHandler
    public void logException(JoinPoint joinPoint,Exception exception){
        Object[] args = joinPoint.getArgs();
        String functionName = joinPoint.getSignature().getName();
//        String msg = ExceptionUtils.getMessage(exception) +
//                " 行数:" +  exception.getStackTrace()[0].getLineNumber() +
//                " 文件:" + exception.getStackTrace()[0].getFileName() +
//                " 类名:" + exception.getStackTrace()[0].getClassName() +
//                " 方法名:" + exception.getStackTrace()[0].getMethodName();
//        exception.printStackTrace();
        logger.error("方法" + functionName + "异常,异常信息:{"+exception+"}");
    }

    /**
     * 统计方法执行耗时Around环绕通知
     */
    @Around(value="pointCutVP()")
    public Object timeAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object obj = joinPoint.proceed(joinPoint.getArgs());
        long endTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取执行的方法名
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        // 打印耗时的信息
        this.printExecTime(methodName, startTime, endTime);
        return obj;
    }
    /**
     * 打印方法执行耗时的信息，如果超过了一定的时间，才打印
     * @param methodName string
     * @param startTime long
     * @param endTime long
     */
    private void printExecTime(String methodName, long startTime, long endTime) {
        long diffTime = endTime - startTime;
        if (diffTime > ONE_MINUTE) {
            logger.warn(methodName + " 方法执行耗时：" + diffTime + "ms");
        }
    }
}
