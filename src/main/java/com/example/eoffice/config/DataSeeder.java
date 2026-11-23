package com.example.eoffice.config;

import com.example.eoffice.entity.DocumentBook;
import com.example.eoffice.entity.DocumentType;
import com.example.eoffice.entity.IncomingDocument;
import com.example.eoffice.entity.IncomingDocumentStatus;
import com.example.eoffice.repository.DocumentBookRepository;
import com.example.eoffice.repository.DocumentTypeRepository;
import com.example.eoffice.repository.IncomingDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("seed")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final IncomingDocumentRepository docRepo;
    private final DocumentBookRepository bookRepo;
    private final DocumentTypeRepository typeRepo;

    @Override
    public void run(String... args) {
        if (docRepo.count() > 10000) {
            System.out.println(">>> Đã có dữ liệu mẫu, bỏ qua.");
            return;
        }
        List<DocumentBook> books = bookRepo.findAllByDeletedFalseOrderByIdAsc();
        List<DocumentType> types = typeRepo.findAll();
        if (books.isEmpty() || types.isEmpty()) {
            System.out.println(">>> Cần có ít nhất 1 sổ văn bản và 1 loại văn bản trước khi tạo dữ liệu mẫu.");
            return;
        }
        DocumentBook book = books.get(0);

        String[] topics = {"kế hoạch ngân sách", "báo cáo tài chính", "đề nghị cấp kinh phí", "tổ chức hội nghị",
                "chuyển đổi số", "cải cách hành chính", "quy hoạch đô thị", "bổ nhiệm cán bộ"};
        String[] organs = {"UBND TP.HCM", "Sở Tài chính", "Bộ Giáo dục và Đào tạo", "Sở Thông tin và Truyền thông"};
        IncomingDocumentStatus[] statuses = IncomingDocumentStatus.values();

        Random r = new Random(1);
        List<IncomingDocument> batch = new ArrayList<>();
        for (int i = 1; i <= 20000; i++) {
            IncomingDocument d = new IncomingDocument();
            d.setDocumentBook(book);
            d.setDocumentType(types.get(r.nextInt(types.size())));
            d.setArrivalNumber(100000L + i);                       // số lớn để không trùng số thật
            LocalDate arrival = LocalDate.of(2026, 1, 1).plusDays(r.nextInt(330));
            d.setArrivalDate(arrival);
            d.setDueDate(arrival.plusDays(7 + r.nextInt(24)));
            d.setSubject("Về việc " + topics[r.nextInt(topics.length)] + " năm 2026 - số " + i);
            d.setOrganName(organs[r.nextInt(organs.length)]);
            d.setCodeNumber(String.valueOf(i));
            d.setCodeNotation(i + "/" + (2020 + r.nextInt(7)) + "-UBND");
            d.setPriority(r.nextInt(4));
            d.setStatus(statuses[r.nextInt(statuses.length)]);     // đa dạng trạng thái để thử bộ lọc sau này
            batch.add(d);
            if (batch.size() == 500) {
                docRepo.saveAll(batch);
                batch.clear();
            }
        }
        docRepo.saveAll(batch);
        System.out.println(">>> Đã tạo xong 20000 văn bản đến mẫu.");
    }
}