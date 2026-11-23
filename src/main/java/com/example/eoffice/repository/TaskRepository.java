package com.example.eoffice.repository;

import com.example.eoffice.dto.DispatchOptionsResponse;
import com.example.eoffice.entity.Task;
import com.example.eoffice.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByIncomingDocumentIdOrderByIdDesc(Long incomingDocumentId);

    @EntityGraph(attributePaths = {
            "parentTask", "taskGroup", "incomingDocument", "creator", "assignee", "supervisors", "supporters"
    })
    @Query("""
            select t from Task t
            left join t.incomingDocument doc
            where (
                (:mode = 'ASSIGNED' and t.assignee.email = :email)
                or (:mode = 'SUPPORTED' and exists (select 1 from t.supporters spt where spt.email = :email))
                or (:mode = 'SUPERVISED' and exists (select 1 from t.supervisors sup where sup.email = :email))
                or (:mode = 'CREATED' and t.creator.email = :email)
            )
            and (:status is null or t.status = :status)
            and (:keyword is null
                 or lower(t.title) like :keyword
                 or (doc is not null and lower(doc.subject) like :keyword))
            order by t.id desc
            """)
    Page<Task> searchTasks(@Param("mode") String mode,
                           @Param("email") String email,
                           @Param("keyword") String keyword,
                           @Param("status") TaskStatus status,
                           Pageable pageable);

    @EntityGraph(attributePaths = {
            "taskGroup", "incomingDocument", "creator", "assignee", "supervisors", "supporters"
    })
    Optional<Task> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "parentTask", "taskGroup", "incomingDocument", "creator", "assignee", "supervisors", "supporters"
    })
    List<Task> findByParentTaskIdOrderByIdAsc(Long parentTaskId);

    Optional<Task> findFirstByIncomingDocumentIdAndParentTaskIsNullOrderByIdDesc(Long incomingDocumentId);

    boolean existsByIncomingDocumentId(Long incomingDocumentId);

}
