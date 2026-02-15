package com.kaede.portfoliobackend.config;

import com.kaede.portfoliobackend.entity.Career;
import com.kaede.portfoliobackend.repository.CareerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(CareerRepository repository) {
        return args -> {
            if (repository.count() == 0) { // 只有数据库为空时才插入
                repository.saveAll(List.of(
                        Career.builder()
                                .period("2020.9 - 2024.6")
                                .company("重庆邮电大学")
                                .position("软件工程 学士")
                                .description("主修课程：⾼等数学、离散数学、线性代数、概率论、数据结构与算法、计算机系统、计算机⽹络、数据库原理与应⽤、技术⽂档编制、软件架构与设计模式、软件分析与设计、软件测试与维护等。")
                                .tags("C++, Java, Spring Boot, Vue").build(),
                        Career.builder()
                                .period("2024.6 - Present")
                                .company("华为技术有限公司成都研究所")
                                .position("软件测试工程师")
                                .description("Conducted version and regression testing, test design, and test environment setup. Developed and maintained automated test scripts using Python and Ruby, and collaborated with cross-functional teams to support system verification and production network mirror testing.")
                                .tags("Ruby, Python, SoftWare Testing, Automated Tesing").build()
                ));
                System.out.println("成功初始化演示数据！");
            }
        };
    }
}
