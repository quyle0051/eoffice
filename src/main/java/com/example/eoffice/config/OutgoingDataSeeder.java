package com.example.eoffice.config;

import com.example.eoffice.entity.*;
import com.example.eoffice.repository.DocumentBookRepository;
import com.example.eoffice.repository.DocumentTypeRepository;
import com.example.eoffice.repository.OutgoingDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@Component
@Profile("seedout")
@RequiredArgsConstructor
public class OutgoingDataSeeder implements CommandLineRunner {
    private final OutgoingDocumentRepository repo;
    private final DocumentBookRepository bookRepo;
    private final DocumentTypeRepository typeRepo;

    @Override
    public void run(String... args) {
        if (repo.count() > 10000) return;
        DocumentBook book = bookRepo.findAllByDeletedFalseOrderByIdAsc().get(0);
        List<DocumentType> types = typeRepo.findAll();
        String[] topics = {"kế hoạch ngân sách", "báo cáo tài chính", "đề nghị cấp kinh phí",
                "tổ chức hội nghị", "chuyển đổi số", "cải cách hành chính"};
        String[] recipients = {"Sở Tài chính TP.HCM", "Bộ Giáo dục và Đào tạo",
                "Ủy ban nhân dân Thành phố Hồ Chí Minh", "Sở Thông tin và Truyền thông TP.HCM"};
        OutgoingDocumentStatus[] statuses = OutgoingDocumentStatus.values();
        Random r = new Random(2);
        List<OutgoingDocument> batch = new ArrayList<>();
        for (int i = 1; i <= 20000; i++) {
            OutgoingDocument d = new OutgoingDocument();
            d.setDocumentBook(book);
            d.setDocumentType(types.get(r.nextInt(types.size())));
            d.setCodeNumber(String.valueOf(i));
            d.setCodeNotation("/QĐ-UBND");
            LocalDate issued = LocalDate.of(2026, 1, 1).plusDays(r.nextInt(330));
            d.setIssuedDate(issued);
            d.setDueDate(issued.plusDays(7 + r.nextInt(24)));
            d.setOrganName("Văn phòng Điện tử E-Office");
            d.setSubject("Về việc " + topics[r.nextInt(topics.length)] + " năm 2026 - số " + i);
            d.setSignerFullName("Nguyễn Văn " + (char) ('A' + r.nextInt(5)));
            d.setToOrgansSummary(recipients[r.nextInt(recipients.length)]);
            d.setPriority(r.nextInt(4));
            d.setStatus(statuses[r.nextInt(statuses.length)]);
            batch.add(d);
            if (batch.size() == 500) { repo.saveAll(batch); batch.clear(); }
        }
        repo.saveAll(batch);
    }
}