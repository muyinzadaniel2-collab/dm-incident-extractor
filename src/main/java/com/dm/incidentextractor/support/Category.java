package com.dm.incidentextractor.support;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * What a customer message is about.
 */
public enum Category {
    /** Something in the product is broken, erroring, or not behaving as expected. */
    @JsonProperty("technical_issue") TECHNICAL_ISSUE,
    /** Login, password reset, 2FA, locked or compromised accounts. */
    @JsonProperty("account_access") ACCOUNT_ACCESS,
    /** Charges, invoices, refunds, subscriptions, payment methods. */
    @JsonProperty("billing") BILLING,
    /** A request for new functionality or a change to existing behaviour. */
    @JsonProperty("feature_request") FEATURE_REQUEST,
    /** A how-to or general question about the product. */
    @JsonProperty("product_question") PRODUCT_QUESTION,
    /** A review, praise, or complaint that does not ask for anything specific. */
    @JsonProperty("feedback") FEEDBACK,
    /** Anything that fits none of the above. */
    @JsonProperty("other") OTHER
}
