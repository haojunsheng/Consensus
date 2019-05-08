package com.anapodoton;

/**
 * 承诺的内容
 * ack：是否接受提案
 * Proposal：提案的内容，包括提案编号和提案内容
 */
public class Promise {
    private final boolean ack;
    private final Proposal proposal;

    public Promise(boolean ack, Proposal proposal) {
        this.ack = ack;
        this.proposal = proposal;
    }

    public boolean isAck() {
        return ack;
    }

    public Proposal getProposal() {
        return proposal;
    }


}
