package com.xhxi.photobooker.service.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xhxi.photobooker.config.RagProperties;
import com.xhxi.photobooker.entity.Photographer;
import com.xhxi.photobooker.entity.PhotographerPackage;
import com.xhxi.photobooker.entity.Portfolio;
import com.xhxi.photobooker.entity.rag.KnowledgeDocument;
import com.xhxi.photobooker.mapper.PhotographerMapper;
import com.xhxi.photobooker.mapper.PhotographerPackageMapper;
import com.xhxi.photobooker.mapper.PortfolioMapper;
import com.xhxi.photobooker.service.ollama.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "rag.enabled", havingValue = "true")
public class KnowledgeIndexService {

    private static final String LAST_INDEX_TIME_KEY = "rag:last-index-time";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final PhotographerMapper photographerMapper;
    private final PhotographerPackageMapper packageMapper;
    private final PortfolioMapper portfolioMapper;
    private final OllamaClient ollamaClient;
    private final QdrantVectorService qdrantVectorService;
    private final ElasticsearchService elasticsearchService;
    private final RagProperties ragProperties;
    private final StringRedisTemplate redisTemplate;

    public KnowledgeIndexService(PhotographerMapper photographerMapper,
                                  PhotographerPackageMapper packageMapper,
                                  PortfolioMapper portfolioMapper,
                                  OllamaClient ollamaClient,
                                  QdrantVectorService qdrantVectorService,
                                  ElasticsearchService elasticsearchService,
                                  RagProperties ragProperties,
                                  StringRedisTemplate redisTemplate) {
        this.photographerMapper = photographerMapper;
        this.packageMapper = packageMapper;
        this.portfolioMapper = portfolioMapper;
        this.ollamaClient = ollamaClient;
        this.qdrantVectorService = qdrantVectorService;
        this.elasticsearchService = elasticsearchService;
        this.ragProperties = ragProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Full rebuild: read all photographers, packages, portfolios from DB,
     * generate embeddings, and index into both Qdrant and Elasticsearch.
     *
     * Note: This method may take a long time. Callers should invoke it asynchronously if needed.
     *
     * @return summary map with counts
     */
    public Map<String, Object> rebuildFullIndex() {
        log.info("Starting full index rebuild...");
        Map<String, Object> summary = new LinkedHashMap<>();
        int photographerCount = 0;
        int packageCount = 0;
        int portfolioCount = 0;

        try {
            // --- Photographers ---
            List<Photographer> photographers = photographerMapper.selectList(null);
            for (Photographer p : photographers) {
                try {
                    String docId = generateDocId("photographer", p.getId());
                    String text = buildPhotographerText(p);
                    Map<String, Object> metadata = buildMetadata("photographer", p.getId(), linkedMapOf(
                            "name", nullSafe(p.getName()),
                            "style", p.getStyle(),
                            "location", p.getLocation(),
                            "latitude", p.getLatitude(),
                            "longitude", p.getLongitude()));
                    indexSingleDocument(docId, text, metadata);
                    photographerCount++;
                    if (photographerCount % 10 == 0) {
                        log.info("Indexed {} photographers...", photographerCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to index photographer id={}: {}", p.getId(), e.getMessage());
                }
            }
            log.info("Indexed {} photographers total", photographerCount);

            // --- Packages ---
            List<PhotographerPackage> packages = packageMapper.selectList(null);
            for (PhotographerPackage pkg : packages) {
                try {
                    String docId = generateDocId("package", pkg.getId());
                    String text = buildPackageText(pkg);
                    Map<String, Object> metadata = buildMetadata("package", pkg.getId(),
                            linkedMapOf("name", nullSafe(pkg.getName()), "photographerId", pkg.getPhotographerId()));
                    indexSingleDocument(docId, text, metadata);
                    packageCount++;
                    if (packageCount % 10 == 0) {
                        log.info("Indexed {} packages...", packageCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to index package id={}: {}", pkg.getId(), e.getMessage());
                }
            }
            log.info("Indexed {} packages total", packageCount);

            // --- Portfolios ---
            // Pre-load photographers to avoid N+1 queries
            Map<Long, Photographer> photographerMap = new HashMap<>();
            photographerMapper.selectList(null).forEach(p -> photographerMap.put(p.getId(), p));

            List<Portfolio> portfolios = portfolioMapper.selectList(null);
            for (Portfolio pf : portfolios) {
                try {
                    String docId = generateDocId("portfolio", pf.getId());
                    String text = buildPortfolioText(pf, photographerMap);
                    Photographer pfOwner = pf.getPhotographerId() != null ? photographerMap.get(pf.getPhotographerId()) : null;
                    Map<String, Object> metadata = buildMetadata("portfolio", pf.getId(),
                            linkedMapOf("title", nullSafe(pf.getTitle()),
                                    "photographerId", pf.getPhotographerId(),
                                    "photographerName", pfOwner != null ? pfOwner.getName() : null,
                                    "category", pf.getCategory(),
                                    "tags", pf.getTags()));
                    indexSingleDocument(docId, text, metadata);
                    portfolioCount++;
                    if (portfolioCount % 10 == 0) {
                        log.info("Indexed {} portfolios...", portfolioCount);
                    }
                } catch (Exception e) {
                    log.error("Failed to index portfolio id={}: {}", pf.getId(), e.getMessage());
                }
            }
            log.info("Indexed {} portfolios total", portfolioCount);

            // Update last index time in Redis
            redisTemplate.opsForValue().set(LAST_INDEX_TIME_KEY, LocalDateTime.now().format(DT_FMT));

        } catch (Exception e) {
            log.error("Error during full index rebuild: {}", e.getMessage(), e);
        }

        summary.put("photographers", photographerCount);
        summary.put("packages", packageCount);
        summary.put("portfolios", portfolioCount);
        summary.put("total", photographerCount + packageCount + portfolioCount);
        summary.put("status", "completed");
        log.info("Full index rebuild completed: {}", summary);
        return summary;
    }

    /**
     * Incremental index: only index records updated since last index time.
     *
     * @return summary of newly indexed records
     */
    public Map<String, Object> incrementalIndex() {
        log.info("Starting incremental index...");
        Map<String, Object> summary = new LinkedHashMap<>();
        int photographerCount = 0;
        int packageCount = 0;
        int portfolioCount = 0;

        try {
            // Get last index time from Redis
            String lastTimeStr = redisTemplate.opsForValue().get(LAST_INDEX_TIME_KEY);
            Date lastIndexDate = null;
            if (lastTimeStr != null && !lastTimeStr.isEmpty()) {
                LocalDateTime lastTime = LocalDateTime.parse(lastTimeStr, DT_FMT);
                lastIndexDate = Date.from(lastTime.atZone(ZoneId.systemDefault()).toInstant());
                log.info("Last index time: {}", lastTimeStr);
            } else {
                log.info("No previous index time found, falling back to full rebuild");
                return rebuildFullIndex();
            }

            // --- Photographers updated since last index ---
            LambdaQueryWrapper<Photographer> photographerQuery = new LambdaQueryWrapper<>();
            photographerQuery.gt(Photographer::getUpdateTime, lastIndexDate);
            List<Photographer> photographers = photographerMapper.selectList(photographerQuery);
            for (Photographer p : photographers) {
                try {
                    String docId = generateDocId("photographer", p.getId());
                    String text = buildPhotographerText(p);
                    Map<String, Object> metadata = buildMetadata("photographer", p.getId(), linkedMapOf(
                            "name", nullSafe(p.getName()),
                            "style", p.getStyle(),
                            "location", p.getLocation(),
                            "latitude", p.getLatitude(),
                            "longitude", p.getLongitude()));
                    indexSingleDocument(docId, text, metadata);
                    photographerCount++;
                } catch (Exception e) {
                    log.error("Failed to index photographer id={}: {}", p.getId(), e.getMessage());
                }
            }

            // --- Packages updated since last index ---
            LambdaQueryWrapper<PhotographerPackage> packageQuery = new LambdaQueryWrapper<>();
            packageQuery.gt(PhotographerPackage::getUpdateTime, lastIndexDate);
            List<PhotographerPackage> packages = packageMapper.selectList(packageQuery);
            for (PhotographerPackage pkg : packages) {
                try {
                    String docId = generateDocId("package", pkg.getId());
                    String text = buildPackageText(pkg);
                    Map<String, Object> metadata = buildMetadata("package", pkg.getId(),
                            linkedMapOf("name", nullSafe(pkg.getName()), "photographerId", pkg.getPhotographerId()));
                    indexSingleDocument(docId, text, metadata);
                    packageCount++;
                } catch (Exception e) {
                    log.error("Failed to index package id={}: {}", pkg.getId(), e.getMessage());
                }
            }

            // --- Portfolios updated since last index ---
            Map<Long, Photographer> photographerMap = new HashMap<>();
            photographerMapper.selectList(null).forEach(p -> photographerMap.put(p.getId(), p));

            LambdaQueryWrapper<Portfolio> portfolioQuery = new LambdaQueryWrapper<>();
            portfolioQuery.gt(Portfolio::getUpdateTime, lastIndexDate);
            List<Portfolio> portfolios = portfolioMapper.selectList(portfolioQuery);
            for (Portfolio pf : portfolios) {
                try {
                    String docId = generateDocId("portfolio", pf.getId());
                    String text = buildPortfolioText(pf, photographerMap);
                    Photographer pfOwner = pf.getPhotographerId() != null ? photographerMap.get(pf.getPhotographerId()) : null;
                    Map<String, Object> metadata = buildMetadata("portfolio", pf.getId(),
                            linkedMapOf("title", nullSafe(pf.getTitle()),
                                    "photographerId", pf.getPhotographerId(),
                                    "photographerName", pfOwner != null ? pfOwner.getName() : null,
                                    "category", pf.getCategory(),
                                    "tags", pf.getTags()));
                    indexSingleDocument(docId, text, metadata);
                    portfolioCount++;
                } catch (Exception e) {
                    log.error("Failed to index portfolio id={}: {}", pf.getId(), e.getMessage());
                }
            }

            // Update last index time
            redisTemplate.opsForValue().set(LAST_INDEX_TIME_KEY, LocalDateTime.now().format(DT_FMT));

        } catch (Exception e) {
            log.error("Error during incremental index: {}", e.getMessage(), e);
        }

        summary.put("newPhotographers", photographerCount);
        summary.put("newPackages", packageCount);
        summary.put("newPortfolios", portfolioCount);
        summary.put("total", photographerCount + packageCount + portfolioCount);
        summary.put("status", "completed");
        log.info("Incremental index completed: {}", summary);
        return summary;
    }

    /**
     * Delete a specific document from both Qdrant and Elasticsearch.
     *
     * @param source  source type (photographer, package, portfolio)
     * @param sourceId the entity id
     */
    public void deleteDocument(String source, Long sourceId) {
        String docId = generateDocId(source, sourceId);
        try {
            qdrantVectorService.deleteDocument(docId);
        } catch (Exception e) {
            log.error("Failed to delete from Qdrant, docId={}: {}", docId, e.getMessage());
        }
        try {
            elasticsearchService.deleteDocument(docId);
        } catch (Exception e) {
            log.error("Failed to delete from Elasticsearch, docId={}: {}", docId, e.getMessage());
        }
        log.info("Deleted document: source={}, sourceId={}", source, sourceId);
    }

    /**
     * Return document counts by source type from Elasticsearch.
     */
    public Map<String, Object> getDocumentCount() {
        Map<String, Object> counts = new LinkedHashMap<>();
        try {
            // Get total from ES index info
            Map<String, Object> indexInfo = elasticsearchService.getIndexInfo();
            counts.put("totalInElasticsearch", indexInfo.getOrDefault("totalDocs", 0L));

            Map<String, Object> qdrantInfo = qdrantVectorService.getCollectionInfo();
            counts.put("totalInQdrant", qdrantInfo.getOrDefault("points_count", 0L));

            // DB counts for reference
            counts.put("photographersInDb", photographerMapper.selectCount(null));
            counts.put("packagesInDb", packageMapper.selectCount(null));
            counts.put("portfoliosInDb", portfolioMapper.selectCount(null));

            String lastTime = redisTemplate.opsForValue().get(LAST_INDEX_TIME_KEY);
            counts.put("lastIndexTime", lastTime != null ? lastTime : "never");
        } catch (Exception e) {
            log.error("Failed to get document counts: {}", e.getMessage());
            counts.put("error", e.getMessage());
        }
        return counts;
    }

    // ==================== Private helpers ====================

    private void indexSingleDocument(String docId, String text, Map<String, Object> metadata) {
        // Generate embedding via Ollama
        float[] embedding = ollamaClient.generateEmbedding(text);

        // Write to Qdrant
        qdrantVectorService.upsertDocument(docId, text, embedding, metadata);

        // Write to Elasticsearch only if enabled (default off)
        if (ragProperties.getElasticsearch().isEnabled()) {
            KnowledgeDocument knowledgeDoc = KnowledgeDocument.builder()
                    .id(docId)
                    .content(text)
                    .source((String) metadata.get("source"))
                    .sourceId((Long) metadata.get("sourceId"))
                    .metadata(metadata)
                    .createdAt(LocalDateTime.now())
                    .build();
            elasticsearchService.indexDocument(knowledgeDoc);
        }
    }

    private String buildPhotographerText(Photographer p) {
        StringBuilder sb = new StringBuilder();
        sb.append("摄影师").append(nullSafe(p.getName()));
        if (p.getStyle() != null) {
            sb.append("，风格：").append(p.getStyle());
        }
        if (p.getLocation() != null) {
            sb.append("，位于").append(p.getLocation());
        }
        if (p.getRating() != null) {
            sb.append("，评分").append(p.getRating());
        }
        if (p.getOrderCount() != null) {
            sb.append("，服务").append(p.getOrderCount()).append("次");
        }
        if (p.getWorkYears() != null) {
            sb.append("，从业").append(p.getWorkYears()).append("年");
        }
        if (p.getIntro() != null) {
            sb.append("。").append(p.getIntro());
        }
        if (p.getLatitude() != null && p.getLongitude() != null) {
            sb.append("。坐标：纬度").append(p.getLatitude()).append("，经度").append(p.getLongitude());
        }
        return sb.toString();
    }

    private String buildPackageText(PhotographerPackage pkg) {
        StringBuilder sb = new StringBuilder();
        sb.append("套餐").append(nullSafe(pkg.getName()));
        if (pkg.getPrice() != null) {
            sb.append("，价格").append(pkg.getPrice()).append("元");
        }
        if (pkg.getCategory() != null) {
            sb.append("，类型").append(pkg.getCategory());
        }
        List<String> includes = new ArrayList<>();
        if (pkg.getDuration() != null) {
            includes.add("拍摄时长" + pkg.getDuration() + "小时");
        }
        if (pkg.getPhotoCount() != null) {
            includes.add("精修照片" + pkg.getPhotoCount() + "张");
        }
        if (!includes.isEmpty()) {
            sb.append("，包含").append(String.join("、", includes));
        }
        if (pkg.getServiceDetails() != null) {
            sb.append("。").append(pkg.getServiceDetails());
        } else if (pkg.getDescription() != null) {
            sb.append("。").append(pkg.getDescription());
        }
        return sb.toString();
    }

    private String buildPortfolioText(Portfolio pf, Map<Long, Photographer> photographerMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("作品集").append(nullSafe(pf.getTitle()));
        // Try to find photographer name for richer text
        if (pf.getPhotographerId() != null && photographerMap != null) {
            Photographer photographer = photographerMap.get(pf.getPhotographerId());
            if (photographer != null && photographer.getName() != null) {
                sb.append("，摄影师").append(photographer.getName());
            }
        }
        if (pf.getCategory() != null) {
            sb.append("，风格").append(pf.getCategory());
        }
        if (pf.getTags() != null) {
            sb.append("，标签：").append(pf.getTags());
        }
        if (pf.getShootingLocation() != null) {
            sb.append("，拍摄地").append(pf.getShootingLocation());
        }
        if (pf.getDescription() != null) {
            sb.append("。").append(pf.getDescription());
        }
        return sb.toString();
    }

    private String generateDocId(String source, Long sourceId) {
        return UUID.nameUUIDFromBytes((source + ":" + sourceId).getBytes()).toString();
    }

    private Map<String, Object> buildMetadata(String source, Long sourceId, Map<String, Object> extra) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", source);
        metadata.put("sourceId", sourceId);
        if (extra != null) {
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                if (entry.getValue() != null) {
                    metadata.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return metadata;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * Build a map that tolerates null values (skips null entries).
     */
    private Map<String, Object> linkedMapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }
}
