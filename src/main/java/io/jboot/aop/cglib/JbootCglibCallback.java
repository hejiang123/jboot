/**
 * Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jboot.aop.cglib;

import com.jfinal.aop.Interceptor;
import com.jfinal.aop.InterceptorManager;
import com.jfinal.aop.Invocation;
import io.jboot.aop.InterceptorBuilderManager;
import io.jboot.utils.ClassUtil;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


class JbootCglibCallback implements MethodInterceptor {

    private static final Set<String> excludedMethodName = buildExcludedMethodName();
    private static final InterceptorManager interMan = InterceptorManager.me();

    @Override
    public Object intercept(Object target, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (excludedMethodName.contains(method.getName())) {
            return methodProxy.invokeSuper(target, args);
        }

        Class<?> targetClass = ClassUtil.getUsefulClass(target.getClass());

        JbootCglibProxyFactory.MethodKey key = JbootCglibProxyFactory.IntersCache.getMethodKey(targetClass, method);
        Interceptor[] inters = JbootCglibProxyFactory.IntersCache.get(key);
        if (inters == null) {
            synchronized (method) {
                if (inters == null) {

                    // jfinal 原生的构建
                    Interceptor[] interceptors = interMan.buildServiceMethodInterceptor(targetClass, method);

                    // builder 再次构建
                    interceptors = InterceptorBuilderManager.me().build(targetClass, method, interceptors);

                    JbootCglibProxyFactory.IntersCache.put(key, interceptors);

                    inters = interceptors;
                }
            }
        }


        Invocation invocation = new Invocation(target, method, inters,
                x -> methodProxy.invokeSuper(target, x), args);

        invocation.invoke();
        return invocation.getReturnValue();
    }


    private static final Set<String> buildExcludedMethodName() {
        Set<String> excludedMethodName = new HashSet<String>(64, 0.25F);
        Method[] methods = Object.class.getDeclaredMethods();
        for (Method m : methods) {
            excludedMethodName.add(m.getName());
        }
        // getClass() registerNatives() can not be enhanced
        // excludedMethodName.remove("getClass");
        // excludedMethodName.remove("registerNatives");
        return excludedMethodName;
    }

}


