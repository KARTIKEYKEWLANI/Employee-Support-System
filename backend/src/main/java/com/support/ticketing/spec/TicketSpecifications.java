package com.support.ticketing.spec;

import com.support.ticketing.entity.Ticket;
import com.support.ticketing.entity.TicketPriority;
import com.support.ticketing.entity.TicketStatus;
import jakarta.persistence.criteria.JoinType;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public class TicketSpecifications {

    public static Specification<Ticket> build(
            Long userId,
            boolean restrictToUser,
            String query,
            TicketStatus status,
            TicketPriority priority,
            Long queueId,
            Long assigneeId,
            String tag,
            boolean includeArchived
    ) {
        return (root, cq, cb) -> {
            root.join("user", JoinType.LEFT);
            var predicate = cb.conjunction();
            if (restrictToUser) {
                predicate = cb.and(predicate, cb.equal(root.get("user").get("id"), userId));
            }
            if (!includeArchived) {
                predicate = cb.and(predicate, cb.equal(root.get("archived"), false));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                predicate = cb.and(predicate,
                        cb.or(
                                cb.like(cb.lower(root.get("title")), like),
                                cb.like(cb.lower(root.get("description")), like)
                        ));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicate = cb.and(predicate, cb.equal(root.get("priority"), priority));
            }
            if (queueId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("queue").get("id"), queueId));
            }
            if (assigneeId != null) {
                var join = root.joinSet("assignees", JoinType.LEFT);
                predicate = cb.and(predicate, cb.equal(join.get("id"), assigneeId));
            }
            if (tag != null && !tag.isBlank()) {
                var join = root.joinSet("tags", JoinType.LEFT);
                predicate = cb.and(predicate, cb.equal(cb.lower(join.get("name")), tag.toLowerCase()));
            }
            cq.distinct(true);
            return predicate;
        };
    }
}
