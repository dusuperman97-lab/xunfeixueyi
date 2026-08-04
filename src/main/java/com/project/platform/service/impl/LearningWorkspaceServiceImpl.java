package com.project.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.platform.dto.learning.KnowledgePointSummaryDTO;
import com.project.platform.dto.learning.LearningContextDTO;
import com.project.platform.dto.learning.ModuleSnapshotDTO;
import com.project.platform.dto.learning.RecommendedActionDTO;
import com.project.platform.dto.learning.ResourceSummaryDTO;
import com.project.platform.dto.learning.SectionLearningOverviewDTO;
import com.project.platform.entity.Chapter;
import com.project.platform.entity.Course;
import com.project.platform.entity.LearningActionEvent;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.ResourcePush;
import com.project.platform.entity.Section;
import com.project.platform.entity.StudentSectionProgress;
import com.project.platform.exception.CustomException;
import com.project.platform.mapper.ChapterMapper;
import com.project.platform.mapper.CourseMapper;
import com.project.platform.mapper.LearningActionEventMapper;
import com.project.platform.mapper.QuestionTestRecordMapper;
import com.project.platform.mapper.ResourcePushMapper;
import com.project.platform.mapper.SectionMapper;
import com.project.platform.mapper.StudentSectionProgressMapper;
import com.project.platform.service.LearningWorkspaceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LearningWorkspaceServiceImpl implements LearningWorkspaceService {

    private static final double WEAK_THRESHOLD = 60.0;

    private final CourseMapper courseMapper;
    private final ChapterMapper chapterMapper;
    private final SectionMapper sectionMapper;
    private final QuestionTestRecordMapper questionTestRecordMapper;
    private final ResourcePushMapper resourcePushMapper;
    private final Map<ProgressKey, StudentSectionProgress> progressStore = new ConcurrentHashMap<>();
    private final List<LearningActionEvent> eventStore = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private StudentSectionProgressMapper studentSectionProgressMapper;

    @Resource
    private LearningActionEventMapper learningActionEventMapper;

    public LearningWorkspaceServiceImpl(CourseMapper courseMapper,
                                        ChapterMapper chapterMapper,
                                        SectionMapper sectionMapper,
                                        QuestionTestRecordMapper questionTestRecordMapper,
                                        ResourcePushMapper resourcePushMapper) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.sectionMapper = sectionMapper;
        this.questionTestRecordMapper = questionTestRecordMapper;
        this.resourcePushMapper = resourcePushMapper;
    }

    @Override
    public LearningContextDTO getContext(Integer studentId, Integer courseId, Integer sectionId,
                                         String activeModule, String activeKnowledgePoint) {
        SectionLearningOverviewDTO overview =
                getSectionOverview(studentId, courseId, sectionId, activeModule, activeKnowledgePoint);
        LearningContextDTO context = new LearningContextDTO();
        context.setStudentId(overview.getStudentId());
        context.setCourseId(overview.getCourseId());
        context.setCourseName(overview.getCourseName());
        context.setChapterId(overview.getChapterId());
        context.setChapterName(overview.getChapterName());
        context.setSectionId(overview.getSectionId());
        context.setSectionName(overview.getSectionName());
        context.setActiveModule(overview.getActiveModule());
        context.setActiveKnowledgePoint(overview.getActiveKnowledgePoint());
        context.setProgressSummary(overview.getProgress());
        context.setWeakKnowledgePoints(overview.getWeakKnowledgePoints());
        context.setNextRecommendedAction(overview.getNextRecommendedAction());
        context.setSectionOverview(overview);
        return context;
    }

    @Override
    public SectionLearningOverviewDTO getSectionOverview(Integer studentId, Integer courseId, Integer sectionId,
                                                         String activeModule, String activeKnowledgePoint) {
        LearningScope scope = resolveScope(courseId, sectionId);
        StudentSectionProgress progress = getOrCreateProgress(studentId, scope.courseId(), scope.chapterId(), sectionId);
        List<KnowledgePointSummaryDTO> knowledgePoints = queryKnowledgePointSummaries(studentId, sectionId);
        List<String> weakKnowledgePoints = mergeWeakKnowledgePoints(progress, knowledgePoints);
        ResourceSummaryDTO resourceSummary = summarizeResources(studentId, scope.courseId(), scope.chapterId(),
                sectionId, activeKnowledgePoint, weakKnowledgePoints);
        RecommendedActionDTO nextAction = chooseNextAction(sectionId, progress, weakKnowledgePoints);
        String sectionState = calculateSectionState(progress, weakKnowledgePoints);

        progress.setWeakKnowledgePoints(new ArrayList<>(weakKnowledgePoints));
        progress.setMasteryState(sectionState);
        progress.setRecommendedNextAction(nextAction.getActionType());
        persistProgress(progress);

        SectionLearningOverviewDTO overview = new SectionLearningOverviewDTO();
        overview.setStudentId(studentId);
        overview.setCourseId(scope.courseId());
        overview.setCourseName(scope.courseName());
        overview.setChapterId(scope.chapterId());
        overview.setChapterName(scope.chapterName());
        overview.setSectionId(sectionId);
        overview.setSectionName(scope.sectionName());
        overview.setActiveModule(defaultText(activeModule, "basic-learning"));
        overview.setActiveKnowledgePoint(activeKnowledgePoint);
        overview.setSectionState(sectionState);
        overview.setProgress(progress);
        overview.setKnowledgePoints(knowledgePoints);
        overview.setWeakKnowledgePoints(weakKnowledgePoints);
        overview.setModuleSnapshots(buildModuleSnapshots(progress, knowledgePoints, resourceSummary));
        overview.setResourceSummary(resourceSummary);
        overview.setNextRecommendedAction(nextAction);
        return overview;
    }

    @Override
    public StudentSectionProgress recordEvent(LearningActionEvent event) {
        require(event != null, "learning event cannot be null");
        require(event.getStudentId() != null, "studentId cannot be null");
        require(event.getSectionId() != null, "sectionId cannot be null");
        LearningScope scope = resolveScope(event.getCourseId(), event.getSectionId());
        Integer chapterId = event.getChapterId() != null ? event.getChapterId() : scope.chapterId();
        StudentSectionProgress progress = getOrCreateProgress(event.getStudentId(), scope.courseId(),
                chapterId, event.getSectionId());

        String type = normalize(event.getEventType());
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(System.currentTimeMillis());
        }
        event.setCourseId(scope.courseId());
        event.setChapterId(chapterId);
        event.setEventType(type);
        eventStore.add(event);

        progress.setLastEventTime(event.getOccurredAt());
        if (event.getLastPosition() != null && !event.getLastPosition().isBlank()) {
            progress.setLastPosition(event.getLastPosition());
        }

        switch (type) {
            case "CONTENT_VIEWED" -> progress.setContentViewed(true);
            case "PRACTICE_SUBMITTED" -> applyPracticeResult(progress, event);
            case "RESOURCE_REVIEWED", "RESOURCE_COMPLETED" -> progress.setResourceReviewed(true);
            case "RESOURCE_OPENED", "GRAPH_VIEWED", "PPT_OPENED", "AI_EXPLANATION_REQUESTED" -> {
                // Lightweight events still refresh activity time and resource learning status.
            }
            default -> {
                // Unknown event types are stored for forward compatibility.
            }
        }
        refreshProgressRecommendation(progress);
        persistProgress(progress);
        persistEvent(event);
        return progress;
    }

    @Override
    public void recordPracticeSubmission(Integer studentId, Integer courseId, Integer sectionId,
                                         List<QuestionTestRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        LearningActionEvent event = new LearningActionEvent();
        event.setStudentId(studentId);
        event.setCourseId(courseId);
        event.setSectionId(sectionId);
        event.setEventType("PRACTICE_SUBMITTED");
        event.setAccuracyRate(calculateAccuracy(records));
        event.setWeakKnowledgePoints(extractWeakKnowledgePoints(records));
        recordEvent(event);
    }

    @Override
    public StudentSectionProgress markSectionCompleted(Integer studentId, Integer sectionId, boolean completed) {
        LearningScope scope = resolveScope(null, sectionId);
        StudentSectionProgress progress = getOrCreateProgress(studentId, scope.courseId(), scope.chapterId(), sectionId);
        progress.setContentViewed(completed || progress.isContentViewed());
        progress.setPracticeCompleted(completed || progress.isPracticeCompleted());
        progress.setCompleted(completed);
        progress.setMasteryState(completed ? "MASTERED" : "IN_PROGRESS");
        progress.setRecommendedNextAction(completed ? "NEXT_SECTION" : "CONTENT_LEARNING");
        progress.setLastEventTime(System.currentTimeMillis());
        persistProgress(progress);
        return progress;
    }

    @Override
    public List<String> listWeakKnowledgePoints(Integer studentId, Integer sectionId) {
        LinkedHashSet<String> weakPoints = new LinkedHashSet<>();
        progressStore.values().stream()
                .filter(progress -> Objects.equals(progress.getStudentId(), studentId))
                .filter(progress -> Objects.equals(progress.getSectionId(), sectionId))
                .findFirst()
                .map(StudentSectionProgress::getWeakKnowledgePoints)
                .ifPresent(weakPoints::addAll);
        for (KnowledgePointSummaryDTO summary : queryKnowledgePointSummaries(studentId, sectionId)) {
            if (summary.isWeak()) {
                weakPoints.add(summary.getKnowledgePoint());
            }
        }
        return new ArrayList<>(weakPoints);
    }

    @Override
    public String resolveResourceLearningStatus(Integer studentId, ResourcePush resource) {
        if (studentId == null || resource == null) {
            return "NOT_STARTED";
        }
        Integer resourceId = resource.getId();
        String resourceUrl = resource.getUrl();
        if (countPersistedResourceEvents(studentId, resourceId, resourceUrl,
                List.of("RESOURCE_COMPLETED", "RESOURCE_REVIEWED")) > 0) {
            return "COMPLETED";
        }
        if (countPersistedResourceEvents(studentId, resourceId, resourceUrl,
                List.of("RESOURCE_OPENED")) > 0) {
            return "OPENED";
        }
        boolean completed = eventStore.stream().anyMatch(event -> sameStudentResource(studentId, resource, event)
                && ("RESOURCE_COMPLETED".equals(normalize(event.getEventType()))
                || "RESOURCE_REVIEWED".equals(normalize(event.getEventType()))));
        if (completed) {
            return "COMPLETED";
        }
        boolean opened = eventStore.stream().anyMatch(event -> sameStudentResource(studentId, resource, event)
                && "RESOURCE_OPENED".equals(normalize(event.getEventType())));
        return opened ? "OPENED" : "NOT_STARTED";
    }

    private StudentSectionProgress getOrCreateProgress(Integer studentId, Integer courseId,
                                                       Integer chapterId, Integer sectionId) {
        ProgressKey key = new ProgressKey(studentId, courseId, chapterId, sectionId);
        return progressStore.computeIfAbsent(key,
                ignored -> loadPersistedProgress(studentId, courseId, chapterId, sectionId)
                        .orElseGet(() -> StudentSectionProgress.create(studentId, courseId, chapterId, sectionId)));
    }

    private Optional<StudentSectionProgress> loadPersistedProgress(Integer studentId, Integer courseId,
                                                                   Integer chapterId, Integer sectionId) {
        if (studentSectionProgressMapper == null) {
            return Optional.empty();
        }
        try {
            Map<String, Object> row = studentSectionProgressMapper.selectByKey(studentId, courseId, chapterId, sectionId);
            if (row == null || row.isEmpty()) {
                return Optional.empty();
            }
            StudentSectionProgress progress = StudentSectionProgress.create(studentId, courseId, chapterId, sectionId);
            progress.setContentViewed(asBoolean(row.get("contentViewed")));
            progress.setPracticeCompleted(asBoolean(row.get("practiceCompleted")));
            progress.setResourceReviewed(asBoolean(row.get("resourceReviewed")));
            progress.setCompleted(asBoolean(row.get("completed")));
            progress.setMasteryState(defaultText(asString(row.get("masteryState")), progress.getMasteryState()));
            progress.setLastPosition(asString(row.get("lastPosition")));
            progress.setRecommendedNextAction(defaultText(asString(row.get("recommendedNextAction")),
                    progress.getRecommendedNextAction()));
            progress.setLastEventTime(asLong(row.get("lastEventTime")));
            progress.setWeakKnowledgePoints(readStringList(row.get("weakKnowledgePointsJson")));
            return Optional.of(progress);
        } catch (Exception e) {
            log.warn("Failed to load persisted learning progress, using memory fallback: studentId={}, sectionId={}",
                    studentId, sectionId, e);
            return Optional.empty();
        }
    }

    private void persistProgress(StudentSectionProgress progress) {
        if (studentSectionProgressMapper == null || progress == null) {
            return;
        }
        try {
            studentSectionProgressMapper.upsert(progress, writeJson(progress.getWeakKnowledgePoints()));
        } catch (Exception e) {
            log.warn("Failed to persist learning progress: studentId={}, sectionId={}",
                    progress.getStudentId(), progress.getSectionId(), e);
        }
    }

    private void persistEvent(LearningActionEvent event) {
        if (learningActionEventMapper == null || event == null) {
            return;
        }
        try {
            learningActionEventMapper.insert(event, writeJson(event.getWeakKnowledgePoints()), writeJson(event.getPayload()));
        } catch (Exception e) {
            log.warn("Failed to persist learning event: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType(), e);
        }
    }

    private int countPersistedResourceEvents(Integer studentId, Integer resourceId, String resourceUrl,
                                             List<String> eventTypes) {
        if (learningActionEventMapper == null || studentId == null
                || (resourceId == null && (resourceUrl == null || resourceUrl.isBlank()))
                || eventTypes == null || eventTypes.isEmpty()) {
            return 0;
        }
        try {
            return learningActionEventMapper.countResourceEvents(studentId, resourceId, resourceUrl, eventTypes);
        } catch (Exception e) {
            log.warn("Failed to query persisted resource learning status: studentId={}, resourceId={}",
                    studentId, resourceId, e);
            return 0;
        }
    }

    private LearningScope resolveScope(Integer courseId, Integer sectionId) {
        Section section = sectionMapper.selectById(sectionId);
        if (section == null) {
            throw new CustomException("Section not found: " + sectionId);
        }
        Chapter chapter = section.getChapterId() == null ? null : chapterMapper.selectById(section.getChapterId());
        if (chapter == null) {
            throw new CustomException("Chapter not found for section: " + sectionId);
        }
        Integer resolvedCourseId = courseId != null ? courseId : chapter.getCourseId();
        Course course = courseMapper.selectById(resolvedCourseId);
        if (course == null) {
            throw new CustomException("Course not found: " + resolvedCourseId);
        }
        return new LearningScope(resolvedCourseId, course.getCourseName(), chapter.getId(),
                chapter.getChapterName(), section.getId(), section.getSectionName());
    }

    private List<KnowledgePointSummaryDTO> queryKnowledgePointSummaries(Integer studentId, Integer sectionId) {
        List<Map<String, Object>> stats = Optional
                .ofNullable(questionTestRecordMapper.selectAccuracyByKnowledgePoint(studentId, sectionId))
                .orElse(List.of());
        return stats.stream()
                .map(this::toKnowledgePointSummary)
                .filter(item -> item.getKnowledgePoint() != null && !item.getKnowledgePoint().isBlank())
                .sorted(Comparator.comparing(KnowledgePointSummaryDTO::isWeak).reversed()
                        .thenComparing(KnowledgePointSummaryDTO::getKnowledgePoint))
                .collect(Collectors.toList());
    }

    private KnowledgePointSummaryDTO toKnowledgePointSummary(Map<String, Object> stat) {
        KnowledgePointSummaryDTO summary = new KnowledgePointSummaryDTO();
        summary.setKnowledgePoint(asString(stat.get("knowledgePoint")));
        summary.setTotalCount(asInt(stat.get("totalCount")));
        summary.setCorrectCount(asInt(stat.get("correctCount")));
        summary.setWrongCount(asInt(stat.get("wrongCount")));
        summary.setAccuracyRate(asDouble(stat.get("accuracyRate")));
        summary.setWeak(summary.getWrongCount() > 0 && summary.getAccuracyRate() < WEAK_THRESHOLD);
        summary.setMasteryState(summary.isWeak() ? "NEEDS_REVIEW" : masteryByAccuracy(summary.getAccuracyRate()));
        return summary;
    }

    private List<String> mergeWeakKnowledgePoints(StudentSectionProgress progress,
                                                  List<KnowledgePointSummaryDTO> knowledgePoints) {
        LinkedHashSet<String> weak = new LinkedHashSet<>();
        if (progress.getWeakKnowledgePoints() != null) {
            weak.addAll(progress.getWeakKnowledgePoints());
        }
        knowledgePoints.stream()
                .filter(KnowledgePointSummaryDTO::isWeak)
                .map(KnowledgePointSummaryDTO::getKnowledgePoint)
                .forEach(weak::add);
        return new ArrayList<>(weak);
    }

    private ResourceSummaryDTO summarizeResources(Integer studentId, Integer courseId, Integer chapterId,
                                                  Integer sectionId, String activeKnowledgePoint,
                                                  List<String> weakKnowledgePoints) {
        List<ResourcePush> resources = Optional.ofNullable(resourcePushMapper.selectByCourseId(courseId))
                .orElse(List.of());
        Map<String, ResourcePush> deduped = new LinkedHashMap<>();
        for (ResourcePush resource : resources) {
            if (resource.getUrl() == null || resource.getUrl().isBlank()) {
                continue;
            }
            deduped.putIfAbsent(resource.getUrl(), resource);
        }
        ResourceSummaryDTO summary = new ResourceSummaryDTO();
        summary.setTotalResourceCount(deduped.size());
        int contextual = 0;
        int remedial = 0;
        int reviewed = 0;
        for (ResourcePush resource : deduped.values()) {
            if (matchesContext(resource, chapterId, sectionId, activeKnowledgePoint)) {
                contextual++;
            }
            if (matchesAnyText(resource, weakKnowledgePoints)) {
                remedial++;
            }
            if ("COMPLETED".equals(resolveResourceLearningStatus(studentId, resource))) {
                reviewed++;
            }
        }
        summary.setContextualResourceCount(contextual);
        summary.setRemedialResourceCount(remedial);
        summary.setReviewedResourceCount(reviewed);
        return summary;
    }

    private List<ModuleSnapshotDTO> buildModuleSnapshots(StudentSectionProgress progress,
                                                         List<KnowledgePointSummaryDTO> knowledgePoints,
                                                         ResourceSummaryDTO resourceSummary) {
        List<ModuleSnapshotDTO> snapshots = new ArrayList<>();
        snapshots.add(new ModuleSnapshotDTO("basic-learning",
                progress.isContentViewed() ? "VIEWED" : "PENDING", 1, "Open content"));
        snapshots.add(new ModuleSnapshotDTO("practice",
                progress.isPracticeCompleted() ? "COMPLETED" : "PENDING",
                knowledgePoints.size(), "Practice current section"));
        snapshots.add(new ModuleSnapshotDTO("resource-push", "AVAILABLE",
                resourceSummary.getContextualResourceCount(), "Review resources"));
        snapshots.add(new ModuleSnapshotDTO("knowledge-graph", "CONTEXT_READY",
                (int) knowledgePoints.stream().filter(KnowledgePointSummaryDTO::isWeak).count(), "Inspect graph"));
        return snapshots;
    }

    private RecommendedActionDTO chooseNextAction(Integer sectionId, StudentSectionProgress progress,
                                                  List<String> weakKnowledgePoints) {
        if (!weakKnowledgePoints.isEmpty()) {
            return new RecommendedActionDTO("REMEDIAL_RESOURCE", "Review remedial resources",
                    "resource-push", sectionId, weakKnowledgePoints.get(0));
        }
        if (!progress.isContentViewed()) {
            return new RecommendedActionDTO("CONTENT_LEARNING", "Complete basic learning",
                    "basic-learning", sectionId, null);
        }
        if (!progress.isPracticeCompleted()) {
            return new RecommendedActionDTO("PRACTICE", "Practice current section",
                    "practice", sectionId, null);
        }
        return new RecommendedActionDTO("NEXT_SECTION", "Continue to next section",
                "basic-learning", sectionId, null);
    }

    private String calculateSectionState(StudentSectionProgress progress, List<String> weakKnowledgePoints) {
        if (!weakKnowledgePoints.isEmpty()) {
            return "NEEDS_REVIEW";
        }
        if (progress.isCompleted()
                || (progress.isContentViewed() && progress.isPracticeCompleted())) {
            return "MASTERED";
        }
        if (progress.isContentViewed()) {
            return "NEEDS_PRACTICE";
        }
        return "NOT_STARTED";
    }

    private void applyPracticeResult(StudentSectionProgress progress, LearningActionEvent event) {
        progress.setPracticeCompleted(true);
        LinkedHashSet<String> weak = new LinkedHashSet<>(progress.getWeakKnowledgePoints());
        if (event.getWeakKnowledgePoints() != null) {
            weak.addAll(event.getWeakKnowledgePoints());
        }
        if (event.getKnowledgePoint() != null && event.getAccuracyRate() != null
                && event.getAccuracyRate() * 100 < WEAK_THRESHOLD) {
            weak.add(event.getKnowledgePoint());
        }
        progress.setWeakKnowledgePoints(new ArrayList<>(weak));
        boolean hasWeak = !weak.isEmpty() || (event.getAccuracyRate() != null
                && event.getAccuracyRate() * 100 < WEAK_THRESHOLD);
        progress.setCompleted(!hasWeak && progress.isContentViewed());
        progress.setMasteryState(hasWeak ? "NEEDS_REVIEW" : "MASTERED");
    }

    private void refreshProgressRecommendation(StudentSectionProgress progress) {
        if (!progress.getWeakKnowledgePoints().isEmpty()) {
            progress.setRecommendedNextAction("REMEDIAL_RESOURCE");
        } else if (!progress.isContentViewed()) {
            progress.setRecommendedNextAction("CONTENT_LEARNING");
        } else if (!progress.isPracticeCompleted()) {
            progress.setRecommendedNextAction("PRACTICE");
        } else {
            progress.setRecommendedNextAction("NEXT_SECTION");
        }
    }

    private Double calculateAccuracy(List<QuestionTestRecord> records) {
        long answered = records.stream()
                .filter(record -> record.getUserAnswer() != null && !record.getUserAnswer().isBlank())
                .count();
        if (answered == 0) {
            return 0.0;
        }
        long correct = records.stream().filter(record -> Boolean.TRUE.equals(record.getIsCorrect())).count();
        return (double) correct / answered;
    }

    private List<String> extractWeakKnowledgePoints(List<QuestionTestRecord> records) {
        Map<String, List<QuestionTestRecord>> byKnowledgePoint = records.stream()
                .filter(record -> record.getKnowledgePoint() != null && !record.getKnowledgePoint().isBlank())
                .collect(Collectors.groupingBy(QuestionTestRecord::getKnowledgePoint, LinkedHashMap::new, Collectors.toList()));
        List<String> weak = new ArrayList<>();
        for (Map.Entry<String, List<QuestionTestRecord>> entry : byKnowledgePoint.entrySet()) {
            List<QuestionTestRecord> grouped = entry.getValue();
            long correct = grouped.stream().filter(record -> Boolean.TRUE.equals(record.getIsCorrect())).count();
            double accuracy = grouped.isEmpty() ? 0.0 : (double) correct * 100 / grouped.size();
            if (accuracy < WEAK_THRESHOLD) {
                weak.add(entry.getKey());
            }
        }
        return weak;
    }

    private boolean sameStudentResource(Integer studentId, ResourcePush resource, LearningActionEvent event) {
        return Objects.equals(event.getStudentId(), studentId)
                && (Objects.equals(event.getResourceId(), resource.getId())
                || (event.getResourceUrl() != null && event.getResourceUrl().equals(resource.getUrl())));
    }

    private boolean matchesContext(ResourcePush resource, Integer chapterId, Integer sectionId,
                                   String activeKnowledgePoint) {
        return Objects.equals(resource.getSectionId(), sectionId)
                || Objects.equals(resource.getChapterId(), chapterId)
                || hasText(activeKnowledgePoint) && text(resource).contains(activeKnowledgePoint.toLowerCase(Locale.ROOT));
    }

    private boolean matchesAnyText(ResourcePush resource, List<String> values) {
        String text = text(resource);
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(text::contains);
    }

    private String text(ResourcePush resource) {
        return (safe(resource.getTitle()) + " " + safe(resource.getDescription()))
                .toLowerCase(Locale.ROOT);
    }

    private String masteryByAccuracy(double accuracyRate) {
        if (accuracyRate >= 90) {
            return "MASTERED";
        }
        if (accuracyRate >= 60) {
            return "VERIFYING";
        }
        return "NEEDS_REVIEW";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize learning context JSON", e);
            return "[]";
        }
    }

    private List<String> readStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(item -> !item.isBlank())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        String text = value.toString();
        if (text.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return new ArrayList<>(List.of(text));
        }
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.parseLong(value.toString());
    }

    private static double asDouble(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private record ProgressKey(Integer studentId, Integer courseId, Integer chapterId, Integer sectionId) {
    }

    private record LearningScope(Integer courseId, String courseName, Integer chapterId,
                                 String chapterName, Integer sectionId, String sectionName) {
    }
}
