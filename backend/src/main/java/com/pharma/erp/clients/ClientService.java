package com.pharma.erp.clients;

import com.pharma.erp.common.web.NotFoundException;
import com.pharma.erp.common.web.PageResponse;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
public class ClientService {

    static final Set<String> CLIENT_SORT_PROPERTIES =
            Set.of("id", "name", "city", "district", "state", "createdAt", "updatedAt");
    static final Set<String> FEEDBACK_SORT_PROPERTIES = Set.of("id", "rating", "createdAt");

    private final ClientRepository clients;
    private final ClientFeedbackRepository feedback;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public ClientService(
            ClientRepository clients, ClientFeedbackRepository feedback, JdbcTemplate jdbc, EntityManager entityManager) {
        this.clients = clients;
        this.feedback = feedback;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> search(String name, String phone, String q, Pageable pageable) {
        requireSortable(pageable, CLIENT_SORT_PROPERTIES);
        return PageResponse.from(
                clients.findAll(ClientSpecifications.search(name, phone, q), pageable), ClientResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientResponse get(Integer id) {
        return ClientResponse.from(load(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = new Client();
        apply(client, request);
        return ClientResponse.from(persist(client));
    }

    @Transactional
    public ClientResponse update(Integer id, ClientRequest request) {
        Client client = load(id);
        apply(client, request);
        return ClientResponse.from(persist(client));
    }

    @Transactional
    public void delete(Integer id) {
        clients.delete(load(id));
        clients.flush();
    }

    @Transactional
    public FeedbackResponse addFeedback(Integer clientId, FeedbackRequest request) {
        load(clientId);
        if (request.employeeId() != null && !staffExists(request.employeeId())) {
            throw invalidField("employeeId", "staff member " + request.employeeId() + " does not exist");
        }
        ClientFeedback saved = feedback.saveAndFlush(
                new ClientFeedback(clientId, request.employeeId(), request.rating(), request.comment()));
        entityManager.refresh(saved);
        return FeedbackResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(Integer clientId, Integer feedbackId) {
        load(clientId);
        return feedback.findByIdAndClientId(feedbackId, clientId)
                .map(FeedbackResponse::from)
                .orElseThrow(() -> NotFoundException.of("Feedback", feedbackId));
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponse> listFeedback(Integer clientId, Pageable pageable) {
        load(clientId);
        requireSortable(pageable, FEEDBACK_SORT_PROPERTIES);
        return PageResponse.from(feedback.findByClientId(clientId, pageable), FeedbackResponse::from);
    }

    @Transactional(readOnly = true)
    public FeedbackSummary feedbackSummary(Integer clientId) {
        load(clientId);
        Double average = feedback.averageRating(clientId);
        Double rounded = average == null ? null : Math.round(average * 100) / 100.0;
        return new FeedbackSummary(clientId, feedback.countByClientId(clientId), rounded);
    }

    /** Flushes and re-reads the row so timestamps reflect the column's (second) precision. */
    private Client persist(Client client) {
        Client saved = clients.saveAndFlush(client);
        entityManager.refresh(saved);
        return saved;
    }

    private Client load(Integer id) {
        return clients.findById(id).orElseThrow(() -> NotFoundException.of("Client", id));
    }

    private boolean staffExists(Integer employeeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM staff WHERE employee_id = ?", Integer.class, employeeId);
        return count != null && count > 0;
    }

    private static void apply(Client client, ClientRequest request) {
        client.setName(request.name());
        client.setAddress(request.address());
        client.setPhone(request.phone());
        client.setEmail(request.email());
        client.setCity(request.city());
        client.setDistrict(request.district());
        client.setState(request.state());
        client.setGstin(request.gstin());
        client.setFeedbackEmail(request.feedbackEmail());
    }

    private static void requireSortable(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw invalidField("sort", "unsupported sort property '" + order.getProperty()
                        + "'; allowed: " + allowed.stream().sorted().toList());
            }
        }
    }

    private static ErrorResponseException invalidField(String field, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", Map.of(field, List.of(message)));
        return new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
    }
}
