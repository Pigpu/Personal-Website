package com.kaede.portfoliobackend.controller;

import com.kaede.portfoliobackend.entity.Paper;
import com.kaede.portfoliobackend.repository.PaperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    @Autowired
    private PaperRepository paperRepository;

    /**
     * 获取论文列表
     * 前端路径: GET /api/papers/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<Paper>> getPaperList() {
        // 直接在数据库层面按日期降序（最新的在前面）提取
        List<Paper> papers = paperRepository.findAll(Sort.by(Sort.Direction.DESC, "writeDate"));
        return ResponseEntity.ok(papers);
    }

    /**
     * 保存或更新论文
     * 前端路径: POST /api/papers/save
     */
    @PostMapping("/save")
    public ResponseEntity<?> savePaper(@RequestBody Paper paper) {
        paperRepository.save(paper);
        return ResponseEntity.ok("保存成功");
    }

    /**
     * 删除论文
     * 前端路径: DELETE /api/papers/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePaper(@PathVariable Long id) {
        paperRepository.deleteById(id);
        return ResponseEntity.ok("删除成功");
    }
}