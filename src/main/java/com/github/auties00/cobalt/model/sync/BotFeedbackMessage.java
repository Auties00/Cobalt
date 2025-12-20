package com.github.auties00.cobalt.model.sync;

import com.github.auties00.cobalt.model.message.model.ChatMessageKey;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

@ProtobufMessage(name = "BotFeedbackMessage")
public final class BotFeedbackMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final ChatMessageKey messageKey;

    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final BotFeedbackKind kind;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String text;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    final long kindNegative;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT64)
    final long kindPositive;

    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    final ReportKind kindReport;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final SideBySideSurveyMetadata sideBySideSurveyMetadata;

    public BotFeedbackMessage(ChatMessageKey messageKey, BotFeedbackKind kind, String text, long kindNegative, long kindPositive, ReportKind kindReport, SideBySideSurveyMetadata sideBySideSurveyMetadata) {
        this.messageKey = messageKey;
        this.kind = kind;
        this.text = text;
        this.kindNegative = kindNegative;
        this.kindPositive = kindPositive;
        this.kindReport = kindReport;
        this.sideBySideSurveyMetadata = sideBySideSurveyMetadata;
    }

    public Optional<ChatMessageKey> messageKey() {
        return Optional.ofNullable(messageKey);
    }

    public Optional<BotFeedbackKind> kind() {
        return Optional.ofNullable(kind);
    }

    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    public long kindNegative() {
        return kindNegative;
    }

    public long kindPositive() {
        return kindPositive;
    }

    public Optional<ReportKind> kindReport() {
        return Optional.ofNullable(kindReport);
    }

    public Optional<SideBySideSurveyMetadata> sideBySideSurveyMetadata() {
        return Optional.ofNullable(sideBySideSurveyMetadata);
    }

    @ProtobufEnum(name = "BotFeedbackMessage.BotFeedbackKind")
    public enum BotFeedbackKind {
        BOT_FEEDBACK_POSITIVE(0),
        BOT_FEEDBACK_NEGATIVE_GENERIC(1),
        BOT_FEEDBACK_NEGATIVE_HELPFUL(2),
        BOT_FEEDBACK_NEGATIVE_INTERESTING(3),
        BOT_FEEDBACK_NEGATIVE_ACCURATE(4),
        BOT_FEEDBACK_NEGATIVE_SAFE(5),
        BOT_FEEDBACK_NEGATIVE_OTHER(6),
        BOT_FEEDBACK_NEGATIVE_REFUSED(7),
        BOT_FEEDBACK_NEGATIVE_NOT_VISUALLY_APPEALING(8),
        BOT_FEEDBACK_NEGATIVE_NOT_RELEVANT_TO_TEXT(9),
        BOT_FEEDBACK_NEGATIVE_PERSONALIZED(10),
        BOT_FEEDBACK_NEGATIVE_CLARITY(11),
        BOT_FEEDBACK_NEGATIVE_DOESNT_LOOK_LIKE_THE_PERSON(12),
        BOT_FEEDBACK_NEGATIVE_HALLUCINATION_INTERNAL_ONLY(13),
        BOT_FEEDBACK_NEGATIVE(14);

        final int index;

        BotFeedbackKind(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    @ProtobufEnum(name = "BotFeedbackMessage.ReportKind")
    public enum ReportKind {
        NONE(0),
        GENERIC(1);

        final int index;

        ReportKind(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }
    }

    @ProtobufMessage(name = "BotFeedbackMessage.SideBySideSurveyMetadata")
    public static final class SideBySideSurveyMetadata {
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String selectedRequestId;

        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        final Integer surveyId;

        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String simonSessionFbid;

        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        final String responseOtid;

        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        final String responseTimestampMsString;

        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        final boolean isSelectedResponsePrimary;

        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        final String messageIdToEdit;

        public SideBySideSurveyMetadata(String selectedRequestId, Integer surveyId, String simonSessionFbid, String responseOtid, String responseTimestampMsString, boolean isSelectedResponsePrimary, String messageIdToEdit) {
            this.selectedRequestId = selectedRequestId;
            this.surveyId = surveyId;
            this.simonSessionFbid = simonSessionFbid;
            this.responseOtid = responseOtid;
            this.responseTimestampMsString = responseTimestampMsString;
            this.isSelectedResponsePrimary = isSelectedResponsePrimary;
            this.messageIdToEdit = messageIdToEdit;
        }

        public Optional<String> selectedRequestId() {
            return Optional.ofNullable(selectedRequestId);
        }

        public Optional<Integer> surveyId() {
            return Optional.ofNullable(surveyId);
        }

        public Optional<String> simonSessionFbid() {
            return Optional.ofNullable(simonSessionFbid);
        }

        public Optional<String> responseOtid() {
            return Optional.ofNullable(responseOtid);
        }

        public Optional<String> responseTimestampMsString() {
            return Optional.ofNullable(responseTimestampMsString);
        }

        public boolean isSelectedResponsePrimary() {
            return isSelectedResponsePrimary;
        }

        public Optional<String> messageIdToEdit() {
            return Optional.ofNullable(messageIdToEdit);
        }
    }
}
