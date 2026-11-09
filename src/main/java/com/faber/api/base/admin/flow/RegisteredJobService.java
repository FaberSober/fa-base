package com.faber.api.base.admin.flow;

import cn.hutool.core.util.ClassUtil;
import com.faber.api.base.admin.vo.ret.RegisteredJobVo;
import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/** 目录展示与执行白名单共用解析规则；弃用标记仅影响展示，不移除历史引用。 */
@Service
public class RegisteredJobService {
    public static Set<Class<? extends BaseJob>> registeredTypes() {
        Set<Class<? extends BaseJob>> types = new HashSet<>();
        for (Class<?> type : ClassUtil.scanPackageByAnnotation("com.faber", FaJob.class)) {
            if (BaseJob.class.isAssignableFrom(type)) types.add(type.asSubclass(BaseJob.class));
        }
        return types;
    }
    public static Set<String> registeredPaths() {
        return registeredTypes().stream().map(Class::getName).collect(Collectors.toSet());
    }
    public List<RegisteredJobVo> catalog() { return describe(registeredTypes()); }
    public static List<RegisteredJobVo> describe(Collection<? extends Class<?>> types) {
        return types.stream().filter(BaseJob.class::isAssignableFrom).filter(type -> type.isAnnotationPresent(FaJob.class))
                .distinct().map(type -> {
                    FaJob job = type.getAnnotation(FaJob.class);
                    String name = job.value().isBlank() ? type.getSimpleName() : job.value().trim();
                    return new RegisteredJobVo(type.getName(), name, job.group().trim(), job.description().trim(),
                            Arrays.stream(job.tags()).map(String::trim).filter(tag -> !tag.isEmpty()).distinct().toList(),
                            job.sort(), type.isAnnotationPresent(Deprecated.class));
                }).sorted(Comparator.comparing(RegisteredJobVo::group).thenComparingInt(RegisteredJobVo::sort)
                        .thenComparing(RegisteredJobVo::name).thenComparing(RegisteredJobVo::clazzPath)).toList();
    }
}
