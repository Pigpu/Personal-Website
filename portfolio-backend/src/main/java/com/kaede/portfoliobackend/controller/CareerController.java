package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.Career;
import com.kaede.portfoliobackend.repository.CareerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/career")
public class CareerController {

    @Autowired
    private CareerRepository careerRepository;

    @GetMapping("/list")
    public List<Career> getCareerList() {
        // 使用带排序的查询方法
        return careerRepository.findAllByOrderByStartDateDesc();
    }

    // 1. 新增或修改 (Save)
    @PostMapping("/save")
    public Career saveCareer(@RequestBody Career career) {
        return careerRepository.save(career);
    }

    // 2. 删除 (Delete)
    @DeleteMapping("/delete/{id}")
    public void deleteCareer(@PathVariable Long id) {
        careerRepository.deleteById(id);
    }
}
