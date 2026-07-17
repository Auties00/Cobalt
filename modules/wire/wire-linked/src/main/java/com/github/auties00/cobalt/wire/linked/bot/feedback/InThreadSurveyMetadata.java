package com.github.auties00.cobalt.wire.linked.bot.feedback;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents metadata for an in-thread survey displayed inline within a WhatsApp
 * AI bot conversation.
 *
 * <p>When Meta wants to gather user feedback about AI bot responses, it displays
 * a survey directly within the conversation thread rather than redirecting the
 * user to an external form. This metadata defines the complete structure of such
 * a survey, including the invitation text shown before the user engages, the
 * questions and selectable options presented during the survey, the submit and
 * continue button labels, a privacy statement, and analytics tracking identifiers
 * used for telemetry.
 *
 * <p>The survey flow typically proceeds as follows:
 * <ol>
 * <li>An invitation is shown with a {@linkplain #invitationHeaderText() header},
 *     {@linkplain #invitationBodyText() body}, and a
 *     {@linkplain #invitationCallToActionText() call-to-action button}.
 * <li>Upon engagement, the {@linkplain #surveyTitle() survey title} and
 *     {@linkplain #questions() questions} are presented starting from
 *     {@linkplain #startQuestionIndex() startQuestionIndex}.
 * <li>The user selects options for each question and submits using the
 *     {@linkplain #surveySubmitButtonText() submit button}.
 * <li>A {@linkplain #feedbackToastText() toast message} confirms that feedback
 *     has been received.
 * </ol>
 *
 * <p>Several field names in this class differ from the WA Web protobuf names
 * for readability: {@code analyticsSessionId} maps to {@code tessaSessionId},
 * {@code surveySessionId} maps to {@code simonSessionId}, {@code surveyId} maps
 * to {@code simonSurveyId}, {@code analyticsRootId} maps to {@code tessaRootId},
 * and {@code analyticsEvent} maps to {@code tessaEvent}.
 */
@ProtobufMessage(name = "InThreadSurveyMetadata")
public final class InThreadSurveyMetadata {
    /**
     * The analytics session identifier used for telemetry tracking of this survey
     * interaction. In WA Web this field is named {@code tessaSessionId} after
     * Meta's Tessa analytics platform.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String analyticsSessionId;

    /**
     * The survey session identifier used to correlate survey responses with a
     * specific session. In WA Web this field is named {@code simonSessionId}
     * after Meta's Simon survey platform.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String surveySessionId;

    /**
     * The unique identifier for this survey definition. In WA Web this field is
     * named {@code simonSurveyId} after Meta's Simon survey platform.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String surveyId;

    /**
     * The analytics root identifier used to trace this survey event back to
     * a root analytics context. In WA Web this field is named
     * {@code tessaRootId} after Meta's Tessa analytics platform.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String analyticsRootId;

    /**
     * The request identifier associated with the AI bot response that triggered this survey,
     * for example {@code "req_20240101_abc"}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String requestId;

    /**
     * The analytics event name used for logging this survey interaction. In
     * WA Web this field is named {@code tessaEvent} after Meta's Tessa
     * analytics platform.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String analyticsEvent;

    /**
     * The header text displayed in the survey invitation before the user engages,
     * for example {@code "We'd love your feedback"}.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String invitationHeaderText;

    /**
     * The body text displayed in the survey invitation providing more context,
     * for example {@code "Help us improve Meta AI by answering a quick question"}.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String invitationBodyText;

    /**
     * The call-to-action button text displayed in the survey invitation,
     * for example {@code "Take Survey"}. In WA Web this field is named
     * {@code invitationCtaText}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String invitationCallToActionText;

    /**
     * The URL that the call-to-action button links to in the survey invitation.
     * In WA Web this field is named {@code invitationCtaUrl}.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    URI invitationCallToActionUrl;

    /**
     * The title displayed at the top of the survey form once the user engages,
     * for example {@code "Quick Feedback"}.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String surveyTitle;

    /**
     * The list of questions presented to the user in this survey.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MESSAGE)
    List<InThreadSurveyQuestion> questions;

    /**
     * The text displayed on the button that advances to the next question in a multi-question survey,
     * for example {@code "Continue"}.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    String surveyContinueButtonText;

    /**
     * The text displayed on the button that submits the survey responses,
     * for example {@code "Submit"}.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String surveySubmitButtonText;

    /**
     * The full privacy statement text displayed to the user during the survey,
     * for example {@code "Your feedback is anonymous and helps improve Meta AI."}.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    String privacyStatementFull;

    /**
     * The structured parts of the privacy statement, allowing segments to contain hyperlinks.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.MESSAGE)
    List<InThreadSurveyPrivacyStatementPart> privacyStatementParts;

    /**
     * The toast notification text shown after the user submits the survey,
     * for example {@code "Thanks for your feedback!"}.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.STRING)
    String feedbackToastText;

    /**
     * The zero-based index of the first question to display when the survey opens.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.INT32)
    Integer startQuestionIndex;

    /**
     * Constructs a new {@code InThreadSurveyMetadata} with the specified values.
     *
     * @param analyticsSessionId        the analytics session identifier, or {@code null}
     * @param surveySessionId           the survey session identifier, or {@code null}
     * @param surveyId                  the unique survey identifier, or {@code null}
     * @param analyticsRootId           the analytics root identifier, or {@code null}
     * @param requestId                 the request identifier for the triggering bot response, or {@code null}
     * @param analyticsEvent            the analytics event name, or {@code null}
     * @param invitationHeaderText      the invitation header text, or {@code null}
     * @param invitationBodyText        the invitation body text, or {@code null}
     * @param invitationCallToActionText the invitation call-to-action button text, or {@code null}
     * @param invitationCallToActionUrl  the invitation call-to-action URL, or {@code null}
     * @param surveyTitle               the survey title, or {@code null}
     * @param questions                 the list of survey questions, or {@code null}
     * @param surveyContinueButtonText  the continue button text, or {@code null}
     * @param surveySubmitButtonText    the submit button text, or {@code null}
     * @param privacyStatementFull      the full privacy statement text, or {@code null}
     * @param privacyStatementParts     the structured privacy statement parts, or {@code null}
     * @param feedbackToastText         the feedback toast text, or {@code null}
     * @param startQuestionIndex        the zero-based start question index, or {@code null}
     */
    InThreadSurveyMetadata(String analyticsSessionId, String surveySessionId, String surveyId, String analyticsRootId, String requestId, String analyticsEvent, String invitationHeaderText, String invitationBodyText, String invitationCallToActionText, URI invitationCallToActionUrl, String surveyTitle, List<InThreadSurveyQuestion> questions, String surveyContinueButtonText, String surveySubmitButtonText, String privacyStatementFull, List<InThreadSurveyPrivacyStatementPart> privacyStatementParts, String feedbackToastText, Integer startQuestionIndex) {
        this.analyticsSessionId = analyticsSessionId;
        this.surveySessionId = surveySessionId;
        this.surveyId = surveyId;
        this.analyticsRootId = analyticsRootId;
        this.requestId = requestId;
        this.analyticsEvent = analyticsEvent;
        this.invitationHeaderText = invitationHeaderText;
        this.invitationBodyText = invitationBodyText;
        this.invitationCallToActionText = invitationCallToActionText;
        this.invitationCallToActionUrl = invitationCallToActionUrl;
        this.surveyTitle = surveyTitle;
        this.questions = questions;
        this.surveyContinueButtonText = surveyContinueButtonText;
        this.surveySubmitButtonText = surveySubmitButtonText;
        this.privacyStatementFull = privacyStatementFull;
        this.privacyStatementParts = privacyStatementParts;
        this.feedbackToastText = feedbackToastText;
        this.startQuestionIndex = startQuestionIndex;
    }

    /**
     * Returns the analytics session identifier used for telemetry tracking.
     *
     * @return an {@code Optional} describing the analytics session identifier, or an empty {@code Optional} if not set
     */
    public Optional<String> analyticsSessionId() {
        return Optional.ofNullable(analyticsSessionId);
    }

    /**
     * Returns the survey session identifier used to correlate survey responses.
     *
     * @return an {@code Optional} describing the survey session identifier, or an empty {@code Optional} if not set
     */
    public Optional<String> surveySessionId() {
        return Optional.ofNullable(surveySessionId);
    }

    /**
     * Returns the unique identifier for this survey definition.
     *
     * @return an {@code Optional} describing the survey identifier, or an empty {@code Optional} if not set
     */
    public Optional<String> surveyId() {
        return Optional.ofNullable(surveyId);
    }

    /**
     * Returns the analytics root identifier used for tracing.
     *
     * @return an {@code Optional} describing the analytics root identifier, or an empty {@code Optional} if not set
     */
    public Optional<String> analyticsRootId() {
        return Optional.ofNullable(analyticsRootId);
    }

    /**
     * Returns the request identifier associated with the AI bot response that triggered this survey.
     *
     * @return an {@code Optional} describing the request identifier, or an empty {@code Optional} if not set
     */
    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }

    /**
     * Returns the analytics event name used for logging this survey interaction.
     *
     * @return an {@code Optional} describing the analytics event name, or an empty {@code Optional} if not set
     */
    public Optional<String> analyticsEvent() {
        return Optional.ofNullable(analyticsEvent);
    }

    /**
     * Returns the header text displayed in the survey invitation.
     *
     * @return an {@code Optional} describing the invitation header text, or an empty {@code Optional} if not set
     */
    public Optional<String> invitationHeaderText() {
        return Optional.ofNullable(invitationHeaderText);
    }

    /**
     * Returns the body text displayed in the survey invitation.
     *
     * @return an {@code Optional} describing the invitation body text, or an empty {@code Optional} if not set
     */
    public Optional<String> invitationBodyText() {
        return Optional.ofNullable(invitationBodyText);
    }

    /**
     * Returns the call-to-action button text displayed in the survey invitation.
     *
     * @return an {@code Optional} describing the invitation call-to-action text, or an empty {@code Optional} if not set
     */
    public Optional<String> invitationCallToActionText() {
        return Optional.ofNullable(invitationCallToActionText);
    }

    /**
     * Returns the URL that the call-to-action button links to in the survey invitation.
     *
     * @return an {@code Optional} describing the invitation call-to-action URL, or an empty {@code Optional} if not set
     */
    public Optional<URI> invitationCallToActionUrl() {
        return Optional.ofNullable(invitationCallToActionUrl);
    }

    /**
     * Returns the title displayed at the top of the survey form.
     *
     * @return an {@code Optional} describing the survey title, or an empty {@code Optional} if not set
     */
    public Optional<String> surveyTitle() {
        return Optional.ofNullable(surveyTitle);
    }

    /**
     * Returns the list of questions presented to the user in this survey.
     *
     * @return an unmodifiable list of survey questions, never {@code null}
     */
    public List<InThreadSurveyQuestion> questions() {
        return questions == null ? List.of() : Collections.unmodifiableList(questions);
    }

    /**
     * Returns the text displayed on the continue button in a multi-question survey.
     *
     * @return an {@code Optional} describing the continue button text, or an empty {@code Optional} if not set
     */
    public Optional<String> surveyContinueButtonText() {
        return Optional.ofNullable(surveyContinueButtonText);
    }

    /**
     * Returns the text displayed on the submit button of the survey.
     *
     * @return an {@code Optional} describing the submit button text, or an empty {@code Optional} if not set
     */
    public Optional<String> surveySubmitButtonText() {
        return Optional.ofNullable(surveySubmitButtonText);
    }

    /**
     * Returns the full privacy statement text displayed to the user during the survey.
     *
     * @return an {@code Optional} describing the full privacy statement, or an empty {@code Optional} if not set
     */
    public Optional<String> privacyStatementFull() {
        return Optional.ofNullable(privacyStatementFull);
    }

    /**
     * Returns the structured parts of the privacy statement, where each part may contain
     * a text segment with an optional hyperlink.
     *
     * @return an unmodifiable list of privacy statement parts, never {@code null}
     */
    public List<InThreadSurveyPrivacyStatementPart> privacyStatementParts() {
        return privacyStatementParts == null ? List.of() : Collections.unmodifiableList(privacyStatementParts);
    }

    /**
     * Returns the toast notification text shown after the user submits the survey.
     *
     * @return an {@code Optional} describing the feedback toast text, or an empty {@code Optional} if not set
     */
    public Optional<String> feedbackToastText() {
        return Optional.ofNullable(feedbackToastText);
    }

    /**
     * Returns the zero-based index of the first question to display when the survey opens.
     *
     * @return an {@code OptionalInt} describing the start question index, or an empty {@code OptionalInt} if not set
     */
    public OptionalInt startQuestionIndex() {
        return startQuestionIndex == null ? OptionalInt.empty() : OptionalInt.of(startQuestionIndex);
    }

    /**
     * Sets the analytics session identifier used for telemetry tracking.
     *
     * @param analyticsSessionId the new analytics session identifier, or {@code null}
     */
    public void setAnalyticsSessionId(String analyticsSessionId) {
        this.analyticsSessionId = analyticsSessionId;
    }

    /**
     * Sets the survey session identifier used to correlate survey responses.
     *
     * @param surveySessionId the new survey session identifier, or {@code null}
     */
    public void setSurveySessionId(String surveySessionId) {
        this.surveySessionId = surveySessionId;
    }

    /**
     * Sets the unique identifier for this survey definition.
     *
     * @param surveyId the new survey identifier, or {@code null}
     */
    public void setSurveyId(String surveyId) {
        this.surveyId = surveyId;
    }

    /**
     * Sets the analytics root identifier used for tracing.
     *
     * @param analyticsRootId the new analytics root identifier, or {@code null}
     */
    public void setAnalyticsRootId(String analyticsRootId) {
        this.analyticsRootId = analyticsRootId;
    }

    /**
     * Sets the request identifier associated with the AI bot response that triggered this survey.
     *
     * @param requestId the new request identifier, or {@code null}
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Sets the analytics event name used for logging this survey interaction.
     *
     * @param analyticsEvent the new analytics event name, or {@code null}
     */
    public void setAnalyticsEvent(String analyticsEvent) {
        this.analyticsEvent = analyticsEvent;
    }

    /**
     * Sets the header text displayed in the survey invitation.
     *
     * @param invitationHeaderText the new invitation header text, or {@code null}
     */
    public void setInvitationHeaderText(String invitationHeaderText) {
        this.invitationHeaderText = invitationHeaderText;
    }

    /**
     * Sets the body text displayed in the survey invitation.
     *
     * @param invitationBodyText the new invitation body text, or {@code null}
     */
    public void setInvitationBodyText(String invitationBodyText) {
        this.invitationBodyText = invitationBodyText;
    }

    /**
     * Sets the call-to-action button text displayed in the survey invitation.
     *
     * @param invitationCallToActionText the new invitation call-to-action text, or {@code null}
     */
    public void setInvitationCallToActionText(String invitationCallToActionText) {
        this.invitationCallToActionText = invitationCallToActionText;
    }

    /**
     * Sets the URL that the call-to-action button links to in the survey invitation.
     *
     * @param invitationCallToActionUrl the new invitation call-to-action URL, or {@code null}
     */
    public void setInvitationCallToActionUrl(URI invitationCallToActionUrl) {
        this.invitationCallToActionUrl = invitationCallToActionUrl;
    }

    /**
     * Sets the title displayed at the top of the survey form.
     *
     * @param surveyTitle the new survey title, or {@code null}
     */
    public void setSurveyTitle(String surveyTitle) {
        this.surveyTitle = surveyTitle;
    }

    /**
     * Sets the list of questions presented to the user in this survey.
     *
     * @param questions the new list of survey questions, or {@code null}
     */
    public void setQuestions(List<InThreadSurveyQuestion> questions) {
        this.questions = questions;
    }

    /**
     * Sets the text displayed on the continue button in a multi-question survey.
     *
     * @param surveyContinueButtonText the new continue button text, or {@code null}
     */
    public void setSurveyContinueButtonText(String surveyContinueButtonText) {
        this.surveyContinueButtonText = surveyContinueButtonText;
    }

    /**
     * Sets the text displayed on the submit button of the survey.
     *
     * @param surveySubmitButtonText the new submit button text, or {@code null}
     */
    public void setSurveySubmitButtonText(String surveySubmitButtonText) {
        this.surveySubmitButtonText = surveySubmitButtonText;
    }

    /**
     * Sets the full privacy statement text displayed to the user during the survey.
     *
     * @param privacyStatementFull the new full privacy statement text, or {@code null}
     */
    public void setPrivacyStatementFull(String privacyStatementFull) {
        this.privacyStatementFull = privacyStatementFull;
    }

    /**
     * Sets the structured parts of the privacy statement.
     *
     * @param privacyStatementParts the new list of privacy statement parts, or {@code null}
     */
    public void setPrivacyStatementParts(List<InThreadSurveyPrivacyStatementPart> privacyStatementParts) {
        this.privacyStatementParts = privacyStatementParts;
    }

    /**
     * Sets the toast notification text shown after the user submits the survey.
     *
     * @param feedbackToastText the new feedback toast text, or {@code null}
     */
    public void setFeedbackToastText(String feedbackToastText) {
        this.feedbackToastText = feedbackToastText;
    }

    /**
     * Sets the zero-based index of the first question to display when the survey opens.
     *
     * @param startQuestionIndex the new start question index, or {@code null}
     */
    public void setStartQuestionIndex(Integer startQuestionIndex) {
        this.startQuestionIndex = startQuestionIndex;
    }

    /**
     * Represents a selectable option within an {@link InThreadSurveyQuestion}.
     *
     * <p>Each option can carry a {@linkplain #stringValue() string value} (used as
     * an identifier), a {@linkplain #numericValue() numeric value} (used for scoring
     * or ordering), or both, along with a localized {@linkplain #textTranslated()
     * display label} shown to the user.
     */
    @ProtobufMessage(name = "InThreadSurveyMetadata.InThreadSurveyOption")
    public static final class InThreadSurveyOption {
        /**
         * The string value of this option, used as the option identifier or raw value,
         * for example {@code "very_satisfied"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String stringValue;

        /**
         * The numeric value of this option, used for scoring or ordering purposes.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        Integer numericValue;

        /**
         * The translated text label displayed to the user for this option,
         * for example {@code "Very Satisfied"}.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String textTranslated;

        /**
         * Constructs a new {@code InThreadSurveyOption} with the specified values.
         *
         * @param stringValue    the string value of this option, or {@code null}
         * @param numericValue   the numeric value of this option, or {@code null}
         * @param textTranslated the translated display text for this option, or {@code null}
         */
        InThreadSurveyOption(String stringValue, Integer numericValue, String textTranslated) {
            this.stringValue = stringValue;
            this.numericValue = numericValue;
            this.textTranslated = textTranslated;
        }

        /**
         * Returns the string value of this option.
         *
         * @return an {@code Optional} describing the string value, or an empty {@code Optional} if not set
         */
        public Optional<String> stringValue() {
            return Optional.ofNullable(stringValue);
        }

        /**
         * Returns the numeric value of this option.
         *
         * @return an {@code OptionalInt} describing the numeric value, or an empty {@code OptionalInt} if not set
         */
        public OptionalInt numericValue() {
            return numericValue == null ? OptionalInt.empty() : OptionalInt.of(numericValue);
        }

        /**
         * Returns the translated text label displayed to the user for this option.
         *
         * @return an {@code Optional} describing the translated text, or an empty {@code Optional} if not set
         */
        public Optional<String> textTranslated() {
            return Optional.ofNullable(textTranslated);
        }

        /**
         * Sets the string value of this option.
         *
         * @param stringValue the new string value, or {@code null}
         */
        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
    }

        /**
         * Sets the numeric value of this option.
         *
         * @param numericValue the new numeric value, or {@code null}
         */
        public void setNumericValue(Integer numericValue) {
            this.numericValue = numericValue;
    }

        /**
         * Sets the translated text label displayed to the user for this option.
         *
         * @param textTranslated the new translated display text, or {@code null}
         */
        public void setTextTranslated(String textTranslated) {
            this.textTranslated = textTranslated;
    }
    }

    /**
     * Represents a segment of the privacy statement displayed during an in-thread
     * survey.
     *
     * <p>Each part consists of a {@linkplain #text() text fragment} and an optional
     * {@linkplain #url() URL}. Parts without a URL are rendered as plain text, while
     * parts with a URL are rendered as hyperlinks. Together, the ordered list of parts
     * composes the full privacy statement with inline links.
     */
    @ProtobufMessage(name = "InThreadSurveyMetadata.InThreadSurveyPrivacyStatementPart")
    public static final class InThreadSurveyPrivacyStatementPart {
        /**
         * The text content of this privacy statement segment,
         * for example {@code "Learn more about our "} or {@code "privacy policy"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String text;

        /**
         * The URL associated with this privacy statement segment, making it a hyperlink,
         * for example {@code "https://www.whatsapp.com/legal/privacy-policy"}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String url;

        /**
         * Constructs a new {@code InThreadSurveyPrivacyStatementPart} with the specified values.
         *
         * @param text the text content of this segment, or {@code null}
         * @param url  the URL for this segment, or {@code null}
         */
        InThreadSurveyPrivacyStatementPart(String text, String url) {
            this.text = text;
            this.url = url;
        }

        /**
         * Returns the text content of this privacy statement segment.
         *
         * @return an {@code Optional} describing the text, or an empty {@code Optional} if not set
         */
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        /**
         * Returns the URL associated with this privacy statement segment.
         *
         * @return an {@code Optional} describing the URL, or an empty {@code Optional} if not set
         */
        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        /**
         * Sets the text content of this privacy statement segment.
         *
         * @param text the new text content, or {@code null}
         */
        public void setText(String text) {
            this.text = text;
    }

        /**
         * Sets the URL associated with this privacy statement segment.
         *
         * @param url the new URL, or {@code null}
         */
        public void setUrl(String url) {
            this.url = url;
    }
    }

    /**
     * Represents a single question within an in-thread survey.
     *
     * <p>Each question has a {@linkplain #questionText() display text}, a unique
     * {@linkplain #questionId() identifier}, and a list of selectable
     * {@linkplain #questionOptions() options} from which the user may choose a
     * response.
     */
    @ProtobufMessage(name = "InThreadSurveyMetadata.InThreadSurveyQuestion")
    public static final class InThreadSurveyQuestion {
        /**
         * The display text of this question shown to the user,
         * for example {@code "How satisfied are you with this response?"}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String questionText;

        /**
         * The unique identifier for this question,
         * for example {@code "satisfaction_q1"}.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String questionId;

        /**
         * The list of selectable options available for this question.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        List<InThreadSurveyOption> questionOptions;

        /**
         * Constructs a new {@code InThreadSurveyQuestion} with the specified values.
         *
         * @param questionText    the display text of this question, or {@code null}
         * @param questionId      the unique identifier for this question, or {@code null}
         * @param questionOptions the list of selectable options, or {@code null}
         */
        InThreadSurveyQuestion(String questionText, String questionId, List<InThreadSurveyOption> questionOptions) {
            this.questionText = questionText;
            this.questionId = questionId;
            this.questionOptions = questionOptions;
        }

        /**
         * Returns the display text of this question shown to the user.
         *
         * @return an {@code Optional} describing the question text, or an empty {@code Optional} if not set
         */
        public Optional<String> questionText() {
            return Optional.ofNullable(questionText);
        }

        /**
         * Returns the unique identifier for this question.
         *
         * @return an {@code Optional} describing the question identifier, or an empty {@code Optional} if not set
         */
        public Optional<String> questionId() {
            return Optional.ofNullable(questionId);
        }

        /**
         * Returns the list of selectable options available for this question.
         *
         * @return an unmodifiable list of survey options, never {@code null}
         */
        public List<InThreadSurveyOption> questionOptions() {
            return questionOptions == null ? List.of() : Collections.unmodifiableList(questionOptions);
        }

        /**
         * Sets the display text of this question shown to the user.
         *
         * @param questionText the new question text, or {@code null}
         */
        public void setQuestionText(String questionText) {
            this.questionText = questionText;
    }

        /**
         * Sets the unique identifier for this question.
         *
         * @param questionId the new question identifier, or {@code null}
         */
        public void setQuestionId(String questionId) {
            this.questionId = questionId;
    }

        /**
         * Sets the list of selectable options available for this question.
         *
         * @param questionOptions the new list of survey options, or {@code null}
         */
        public void setQuestionOptions(List<InThreadSurveyOption> questionOptions) {
            this.questionOptions = questionOptions;
    }
    }
}
