package com.example.k5_iot_springboot.service;

import com.example.k5_iot_springboot.entity.Notice;
import com.example.k5_iot_springboot.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public Notice create(String title, String content, String author) {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .author(author)
                .createdAt(LocalDateTime.now())
                .build();

        return noticeRepository.save(notice);
    }

    public List<Notice> getAll() {
        return noticeRepository.findAll();
    }

    public Notice getById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 id의 공지를 찾을 수 없습니다."));
    }

    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }
}