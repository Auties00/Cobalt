package com.github.auties00.cobalt.wire.linked.bot.rendering;

import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Metadata for the progress indicator displayed while the AI bot generates its
 * response.
 *
 * <p>This metadata drives the "thinking" UI that shows the user what the bot
 * is doing while it formulates a reply. It includes:
 * <ul>
 * <li>A {@linkplain #progressDescription() human-readable description} of the
 *     current activity.
 * <li>A list of {@linkplain #stepsMetadata() planning steps} that break down
 *     the bot's reasoning and search process into discrete stages.
 * <li>An {@linkplain #estimatedCompletionTime() estimated completion time} for
 *     the response.
 * </ul>
 *
 * <p>This type is referenced from
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} as the
 * {@code progressIndicatorMetadata} field (protobuf index 12).
 */
@ProtobufMessage(name = "BotProgressIndicatorMetadata")
public final class BotProgressIndicatorMetadata {
    /**
     * A human-readable description of what the bot is currently doing, for
     * example {@code "Searching the web..."} or {@code "Analyzing your question..."}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String progressDescription;

    /**
     * The list of planning steps that break down the bot's reasoning and
     * search process.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<BotPlanningStepMetadata> stepsMetadata;

    /**
     * The estimated time at which the bot will complete its response,
     * represented in epoch milliseconds.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    Instant estimatedCompletionTime;


    /**
     * Constructs a new {@code BotProgressIndicatorMetadata} with the specified
     * values.
     *
     * @param progressDescription    the progress description text, or {@code null}
     * @param stepsMetadata          the list of planning steps, or {@code null}
     * @param estimatedCompletionTime the estimated completion time, or {@code null}
     */
    BotProgressIndicatorMetadata(String progressDescription, List<BotPlanningStepMetadata> stepsMetadata, Instant estimatedCompletionTime) {
        this.progressDescription = progressDescription;
        this.stepsMetadata = stepsMetadata;
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    /**
     * Returns the human-readable description of the current bot activity.
     *
     * @return an {@code Optional} describing the progress text, or an empty
     *         {@code Optional} if not set
     */
    public Optional<String> progressDescription() {
        return Optional.ofNullable(progressDescription);
    }

    /**
     * Returns an unmodifiable view of the planning steps.
     *
     * @return the list of planning step metadata, never {@code null}
     */
    public List<BotPlanningStepMetadata> stepsMetadata() {
        return stepsMetadata == null ? List.of() : Collections.unmodifiableList(stepsMetadata);
    }

    /**
     * Returns the estimated time at which the bot will complete its response.
     *
     * @return an {@code Optional} describing the estimated completion time, or
     *         an empty {@code Optional} if not set
     */
    public Optional<Instant> estimatedCompletionTime() {
        return Optional.ofNullable(estimatedCompletionTime);
    }

    /**
     * Sets the human-readable description of the current bot activity.
     *
     * @param progressDescription the new progress description, or {@code null}
     */
    public void setProgressDescription(String progressDescription) {
        this.progressDescription = progressDescription;
    }

    /**
     * Sets the list of planning steps.
     *
     * @param stepsMetadata the new list of planning steps, or {@code null}
     */
    public void setStepsMetadata(List<BotPlanningStepMetadata> stepsMetadata) {
        this.stepsMetadata = stepsMetadata;
    }

    /**
     * Sets the estimated time at which the bot will complete its response.
     *
     * @param estimatedCompletionTime the new estimated completion time, or {@code null}
     */
    public void setEstimatedCompletionTime(Instant estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    /**
     * Metadata for a single planning step in the bot's reasoning process,
     * representing a discrete stage such as "Searching the web" or "Analyzing
     * results".
     *
     * <p>Each step has a {@linkplain #status() lifecycle status} that
     * progresses from {@link PlanningStepStatus#PLANNED PLANNED} through
     * {@link PlanningStepStatus#EXECUTING EXECUTING} to
     * {@link PlanningStepStatus#FINISHED FINISHED}. Steps may include
     * {@linkplain #sourcesMetadata() search sources} and
     * {@linkplain #sections() sub-sections} with more detailed information.
     */
    @ProtobufMessage(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata")
    public static final class BotPlanningStepMetadata {
        /**
         * The title of this planning step, for example
         * {@code "Searching for recent news"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String statusTitle;

        /**
         * The body text providing additional detail about this step, for
         * example {@code "Found 5 relevant articles"}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String statusBody;

        /**
         * The list of search sources consulted during this planning step.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        List<BotPlanningSearchSourcesMetadata> sourcesMetadata;

        /**
         * The current lifecycle status of this planning step.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
        BotPlanningStepMetadata.PlanningStepStatus status;

        /**
         * Whether this step involves AI reasoning (extended "thinking" mode).
         */
        @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
        Boolean isReasoning;

        /**
         * Whether this step uses an enhanced (deeper) web search.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        Boolean isEnhancedSearch;

        /**
         * The sub-sections within this planning step, providing more granular
         * breakdowns of the work being done.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        List<BotPlanningStepSectionMetadata> sections;


        /**
         * Constructs a new {@code BotPlanningStepMetadata} with the specified values.
         *
         * @param statusTitle      the step title, or {@code null}
         * @param statusBody       the step body text, or {@code null}
         * @param sourcesMetadata  the search sources, or {@code null}
         * @param status           the step status, or {@code null}
         * @param isReasoning      whether reasoning mode is active, or {@code null}
         * @param isEnhancedSearch whether enhanced search is active, or {@code null}
         * @param sections         the step sub-sections, or {@code null}
         */
        BotPlanningStepMetadata(String statusTitle, String statusBody, List<BotPlanningSearchSourcesMetadata> sourcesMetadata, PlanningStepStatus status, Boolean isReasoning, Boolean isEnhancedSearch, List<BotPlanningStepSectionMetadata> sections) {
            this.statusTitle = statusTitle;
            this.statusBody = statusBody;
            this.sourcesMetadata = sourcesMetadata;
            this.status = status;
            this.isReasoning = isReasoning;
            this.isEnhancedSearch = isEnhancedSearch;
            this.sections = sections;
        }

        /**
         * Returns the title of this planning step.
         *
         * @return an {@code Optional} describing the step title, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> statusTitle() {
            return Optional.ofNullable(statusTitle);
        }

        /**
         * Returns the body text of this planning step.
         *
         * @return an {@code Optional} describing the step body, or an empty
         *         {@code Optional} if not set
         */
        public Optional<String> statusBody() {
            return Optional.ofNullable(statusBody);
        }

        /**
         * Returns an unmodifiable view of the search sources consulted during
         * this step.
         *
         * @return the list of search source metadata, never {@code null}
         */
        public List<BotPlanningSearchSourcesMetadata> sourcesMetadata() {
            return sourcesMetadata == null ? List.of() : Collections.unmodifiableList(sourcesMetadata);
        }

        /**
         * Returns the lifecycle status of this planning step.
         *
         * @return an {@code Optional} describing the step status, or an empty
         *         {@code Optional} if not set
         */
        public Optional<PlanningStepStatus> status() {
            return Optional.ofNullable(status);
        }

        /**
         * Returns whether this step involves AI reasoning mode.
         *
         * @return {@code true} if reasoning mode is active, {@code false}
         *         otherwise
         */
        public boolean isReasoning() {
            return isReasoning != null && isReasoning;
        }

        /**
         * Returns whether this step uses enhanced web search.
         *
         * @return {@code true} if enhanced search is active, {@code false}
         *         otherwise
         */
        public boolean isEnhancedSearch() {
            return isEnhancedSearch != null && isEnhancedSearch;
        }

        /**
         * Returns an unmodifiable view of the sub-sections within this step.
         *
         * @return the list of step sections, never {@code null}
         */
        public List<BotPlanningStepSectionMetadata> sections() {
            return sections == null ? List.of() : Collections.unmodifiableList(sections);
        }

        /**
         * Sets the title of this planning step.
         *
         * @param statusTitle the new step title, or {@code null}
         */
        public void setStatusTitle(String statusTitle) {
            this.statusTitle = statusTitle;
    }

        /**
         * Sets the body text of this planning step.
         *
         * @param statusBody the new step body, or {@code null}
         */
        public void setStatusBody(String statusBody) {
            this.statusBody = statusBody;
    }

        /**
         * Sets the list of search sources consulted during this step.
         *
         * @param sourcesMetadata the new search sources list, or {@code null}
         */
        public void setSourcesMetadata(List<BotPlanningSearchSourcesMetadata> sourcesMetadata) {
            this.sourcesMetadata = sourcesMetadata;
    }

        /**
         * Sets the lifecycle status of this planning step.
         *
         * @param status the new step status, or {@code null}
         */
        public void setStatus(PlanningStepStatus status) {
            this.status = status;
    }

        /**
         * Sets whether this step involves AI reasoning mode.
         *
         * @param isReasoning whether reasoning mode is active, or {@code null}
         */
        public void setReasoning(Boolean isReasoning) {
            this.isReasoning = isReasoning;
    }

        /**
         * Sets whether this step uses enhanced web search.
         *
         * @param isEnhancedSearch whether enhanced search is active, or {@code null}
         */
        public void setEnhancedSearch(Boolean isEnhancedSearch) {
            this.isEnhancedSearch = isEnhancedSearch;
    }

        /**
         * Sets the sub-sections within this step.
         *
         * @param sections the new list of step sections, or {@code null}
         */
        public void setSections(List<BotPlanningStepSectionMetadata> sections) {
            this.sections = sections;
    }

        /**
         * The search engine provider that supplied a search source result
         * within a {@link BotPlanningSearchSourceMetadata}.
         */
        @ProtobufEnum(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata.BotSearchSourceProvider")
        public static enum BotSearchSourceProvider {
            /**
             * An unknown or unrecognized search provider.
             */
            UNKNOWN_PROVIDER(0),

            /**
             * A search provider not covered by the other constants.
             */
            OTHER(1),

            /**
             * The Google search engine.
             */
            GOOGLE(2),

            /**
             * The Bing search engine.
             */
            BING(3);

            /**
             * Constructs a new {@code BotSearchSourceProvider} with the given protobuf index.
             *
             * @param index the protobuf enum index
             */
            BotSearchSourceProvider(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf enum index for this search provider.
             */
            final int index;

            /**
             * Returns the protobuf index of this search provider.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * The lifecycle status of a planning step within the bot's reasoning
         * process.
         */
        @ProtobufEnum(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata.PlanningStepStatus")
        public static enum PlanningStepStatus {
            /**
             * The step status is unknown or not yet determined.
             */
            UNKNOWN(0),

            /**
             * The step has been planned but has not yet started executing.
             */
            PLANNED(1),

            /**
             * The step is currently being executed.
             */
            EXECUTING(2),

            /**
             * The step has finished executing.
             */
            FINISHED(3);

            /**
             * Constructs a new {@code PlanningStepStatus} with the given protobuf index.
             *
             * @param index the protobuf enum index
             */
            PlanningStepStatus(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The protobuf enum index for this step status.
             */
            final int index;

            /**
             * Returns the protobuf index of this step status.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * Metadata for a single search source referenced within a
         * {@link BotPlanningStepSectionMetadata}, providing the source title,
         * URL, search provider, and favicon.
         */
        @ProtobufMessage(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata.BotPlanningSearchSourceMetadata")
        public static final class BotPlanningSearchSourceMetadata {
            /**
             * The title of the search source, for example
             * {@code "Wikipedia - Quantum Computing"}.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String title;

            /**
             * The search engine provider that supplied this result.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
            BotPlanningStepMetadata.BotSearchSourceProvider provider;

            /**
             * The URL of the search source, for example
             * {@code "https://en.wikipedia.org/wiki/Quantum_computing"}.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            URI sourceUrl;

            /**
             * The URL of the source's favicon image, for example
             * {@code "https://en.wikipedia.org/favicon.ico"}.
             */
            @ProtobufProperty(index = 4, type = ProtobufType.STRING)
            URI favIconUrl;


            /**
             * Constructs a new {@code BotPlanningSearchSourceMetadata} with the
             * specified values.
             *
             * @param title      the source title, or {@code null}
             * @param provider   the search provider, or {@code null}
             * @param sourceUrl  the source URL, or {@code null}
             * @param favIconUrl the favicon URL, or {@code null}
             */
            BotPlanningSearchSourceMetadata(String title, BotSearchSourceProvider provider, URI sourceUrl, URI favIconUrl) {
                this.title = title;
                this.provider = provider;
                this.sourceUrl = sourceUrl;
                this.favIconUrl = favIconUrl;
            }

            /**
             * Returns the title of the search source.
             *
             * @return an {@code Optional} describing the source title, or an
             *         empty {@code Optional} if not set
             */
            public Optional<String> title() {
                return Optional.ofNullable(title);
            }

            /**
             * Returns the search engine provider.
             *
             * @return an {@code Optional} describing the provider, or an empty
             *         {@code Optional} if not set
             */
            public Optional<BotSearchSourceProvider> provider() {
                return Optional.ofNullable(provider);
            }

            /**
             * Returns the URL of the search source.
             *
             * @return an {@code Optional} describing the source URL, or an
             *         empty {@code Optional} if not set
             */
            public Optional<URI> sourceUrl() {
                return Optional.ofNullable(sourceUrl);
            }

            /**
             * Returns the URL of the source's favicon image.
             *
             * @return an {@code Optional} describing the favicon URL, or an
             *         empty {@code Optional} if not set
             */
            public Optional<URI> favIconUrl() {
                return Optional.ofNullable(favIconUrl);
            }

            /**
             * Sets the title of the search source.
             *
             * @param title the new source title, or {@code null}
             */
            public void setTitle(String title) {
                this.title = title;
    }

            /**
             * Sets the search engine provider.
             *
             * @param provider the new provider, or {@code null}
             */
            public void setProvider(BotSearchSourceProvider provider) {
                this.provider = provider;
    }

            /**
             * Sets the URL of the search source.
             *
             * @param sourceUrl the new source URL, or {@code null}
             */
            public void setSourceUrl(URI sourceUrl) {
                this.sourceUrl = sourceUrl;
    }

            /**
             * Sets the URL of the source's favicon image.
             *
             * @param favIconUrl the new favicon URL, or {@code null}
             */
            public void setFavIconUrl(URI favIconUrl) {
                this.favIconUrl = favIconUrl;
    }
        }

        /**
         * Metadata for a search source referenced directly within a
         * {@link BotPlanningStepMetadata}, providing the source title, URL,
         * and search provider.
         *
         * <p>This is a simplified variant of
         * {@link BotPlanningSearchSourceMetadata} without the favicon URL,
         * used at the step level rather than the section level.
         */
        @ProtobufMessage(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata.BotPlanningSearchSourcesMetadata")
        public static final class BotPlanningSearchSourcesMetadata {
            /**
             * The title of the search source, for example
             * {@code "BBC News - Technology"}.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String sourceTitle;

            /**
             * The search engine provider that supplied this result.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
            BotPlanningStepMetadata.BotPlanningSearchSourcesMetadata.BotPlanningSearchSourceProvider provider;

            /**
             * The URL of the search source, for example
             * {@code "https://www.bbc.com/news/technology"}.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            URI sourceUrl;


            /**
             * Constructs a new {@code BotPlanningSearchSourcesMetadata} with
             * the specified values.
             *
             * @param sourceTitle the source title, or {@code null}
             * @param provider    the search provider, or {@code null}
             * @param sourceUrl   the source URL, or {@code null}
             */
            BotPlanningSearchSourcesMetadata(String sourceTitle, BotPlanningSearchSourceProvider provider, URI sourceUrl) {
                this.sourceTitle = sourceTitle;
                this.provider = provider;
                this.sourceUrl = sourceUrl;
            }

            /**
             * Returns the title of the search source.
             *
             * @return an {@code Optional} describing the source title, or an
             *         empty {@code Optional} if not set
             */
            public Optional<String> sourceTitle() {
                return Optional.ofNullable(sourceTitle);
            }

            /**
             * Returns the search engine provider.
             *
             * @return an {@code Optional} describing the provider, or an empty
             *         {@code Optional} if not set
             */
            public Optional<BotPlanningSearchSourceProvider> provider() {
                return Optional.ofNullable(provider);
            }

            /**
             * Returns the URL of the search source.
             *
             * @return an {@code Optional} describing the source URL, or an
             *         empty {@code Optional} if not set
             */
            public Optional<URI> sourceUrl() {
                return Optional.ofNullable(sourceUrl);
            }

            /**
             * Sets the title of the search source.
             *
             * @param sourceTitle the new source title, or {@code null}
             */
            public void setSourceTitle(String sourceTitle) {
                this.sourceTitle = sourceTitle;
    }

            /**
             * Sets the search engine provider.
             *
             * @param provider the new provider, or {@code null}
             */
            public void setProvider(BotPlanningSearchSourceProvider provider) {
                this.provider = provider;
    }

            /**
             * Sets the URL of the search source.
             *
             * @param sourceUrl the new source URL, or {@code null}
             */
            public void setSourceUrl(URI sourceUrl) {
                this.sourceUrl = sourceUrl;
    }

            /**
             * The search engine provider that supplied a search source result
             * within a {@link BotPlanningSearchSourcesMetadata}.
             */
            @ProtobufEnum(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata.BotPlanningSearchSourcesMetadata.BotPlanningSearchSourceProvider")
            public static enum BotPlanningSearchSourceProvider {
                /**
                 * An unknown or unrecognized search provider.
                 */
                UNKNOWN(0),

                /**
                 * A search provider not covered by the other constants.
                 */
                OTHER(1),

                /**
                 * The Google search engine.
                 */
                GOOGLE(2),

                /**
                 * The Bing search engine.
                 */
                BING(3);

                /**
                 * Constructs a new {@code BotPlanningSearchSourceProvider} with the given protobuf index.
                 *
                 * @param index the protobuf enum index
                 */
                BotPlanningSearchSourceProvider(@ProtobufEnumIndex int index) {
                    this.index = index;
                }

                /**
                 * The protobuf enum index for this search provider.
                 */
                final int index;

                /**
                 * Returns the protobuf index of this search provider.
                 *
                 * @return the protobuf index
                 */
                public int index() {
                    return this.index;
                }
            }
        }

        /**
         * A sub-section within a planning step, providing a more granular
         * breakdown of the work being done, along with search sources
         * referenced in that section.
         */
        @ProtobufMessage(name = "BotProgressIndicatorMetadata.BotPlanningStepMetadata.BotPlanningStepSectionMetadata")
        public static final class BotPlanningStepSectionMetadata {
            /**
             * The title of this section, for example
             * {@code "Analyzing search results"}.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String sectionTitle;

            /**
             * The body text of this section providing additional detail.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String sectionBody;

            /**
             * The list of search sources referenced within this section.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
            List<BotPlanningSearchSourceMetadata> sourcesMetadata;


            /**
             * Constructs a new {@code BotPlanningStepSectionMetadata} with the
             * specified values.
             *
             * @param sectionTitle    the section title, or {@code null}
             * @param sectionBody     the section body text, or {@code null}
             * @param sourcesMetadata the search sources, or {@code null}
             */
            BotPlanningStepSectionMetadata(String sectionTitle, String sectionBody, List<BotPlanningSearchSourceMetadata> sourcesMetadata) {
                this.sectionTitle = sectionTitle;
                this.sectionBody = sectionBody;
                this.sourcesMetadata = sourcesMetadata;
            }

            /**
             * Returns the title of this section.
             *
             * @return an {@code Optional} describing the section title, or an
             *         empty {@code Optional} if not set
             */
            public Optional<String> sectionTitle() {
                return Optional.ofNullable(sectionTitle);
            }

            /**
             * Returns the body text of this section.
             *
             * @return an {@code Optional} describing the section body, or an
             *         empty {@code Optional} if not set
             */
            public Optional<String> sectionBody() {
                return Optional.ofNullable(sectionBody);
            }

            /**
             * Returns an unmodifiable view of the search sources referenced in
             * this section.
             *
             * @return the list of search source metadata, never {@code null}
             */
            public List<BotPlanningSearchSourceMetadata> sourcesMetadata() {
                return sourcesMetadata == null ? List.of() : Collections.unmodifiableList(sourcesMetadata);
            }

            /**
             * Sets the title of this section.
             *
             * @param sectionTitle the new section title, or {@code null}
             */
            public void setSectionTitle(String sectionTitle) {
                this.sectionTitle = sectionTitle;
    }

            /**
             * Sets the body text of this section.
             *
             * @param sectionBody the new section body, or {@code null}
             */
            public void setSectionBody(String sectionBody) {
                this.sectionBody = sectionBody;
    }

            /**
             * Sets the search sources referenced in this section.
             *
             * @param sourcesMetadata the new list of search sources, or {@code null}
             */
            public void setSourcesMetadata(List<BotPlanningSearchSourceMetadata> sourcesMetadata) {
                this.sourcesMetadata = sourcesMetadata;
    }
        }
    }
}
