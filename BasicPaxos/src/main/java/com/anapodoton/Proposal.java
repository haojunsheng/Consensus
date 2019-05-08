package com.anapodoton;

import com.google.common.base.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.anapodoton.BasicPaxos.*;


/**
 * 提案
 */
public class Proposal implements Comparable<Proposal> {
    private final long voteNumber;
    private final String content;

    //提案编号，提案内容
    public Proposal(long voteNumber, String content) {
        this.voteNumber = voteNumber;
        this.content = content;
    }

    public Proposal() {
        this(0, null);
    }

    public long getVoteNumber() {
        return voteNumber;
    }

    public String getContent() {
        return content;
    }

    @Override
    public int compareTo(Proposal o) {
        return Long.compare(voteNumber, o.voteNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Proposal))
            return false;
        Proposal proposal = (Proposal) obj;
        return voteNumber == proposal.voteNumber && StringUtils.equals(content, proposal.content);
    }

    @Override
    public int hashCode() {
        return HASH_FUNCTION
                .newHasher()
                .putLong(voteNumber)
                .putString(content, Charsets.UTF_8)
                .hash()
                .asInt();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(voteNumber)
                .append(':')
                .append(content)
                .toString();
    }

    /**
     * 对于提案的约束，第三条约束要求：
     * 如果maxVote不存在，那么没有限制，下一次表决可以使用任意提案；
     * 否则，下一次表决要沿用maxVote的提案
     *
     * @param currentVoteNumber
     * @param proposals
     * @return
     */
    public static Proposal nextProposal(long currentVoteNumber, List<Proposal> proposals) {
        long voteNumber = currentVoteNumber + 1;
        //如果当前没有提案，则随机生成一个提案
        if (proposals.isEmpty())
            return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
        //先排序，然后找最大提案
        Collections.sort(proposals);
        Proposal maxVote = proposals.get(proposals.size() - 1);
        long maxVoteNumber = maxVote.getVoteNumber();
        String content = maxVote.getContent();
        if (maxVoteNumber >= currentVoteNumber)
            throw new IllegalStateException("illegal state maxVoteNumber");
        if (content != null)
            return new Proposal(voteNumber, content);
        else
            return new Proposal(voteNumber, PROPOSALS[RANDOM.nextInt(PROPOSALS.length)]);
    }
}
