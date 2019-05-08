package com.anapodoton.role;

import com.anapodoton.Promise;
import com.anapodoton.Proposal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static com.anapodoton.BasicPaxos.printInfo;
import static com.anapodoton.Proposal.nextProposal;

/**
 * Proposer,提案者
 *
 */
public class Proposer {

    /**
     * Proposer向Acceptor发出提案
     * @param proposal
     * @param acceptors
     */
    public static void vote(Proposal proposal, Collection<Acceptor> acceptors) {
        //quorum表示最少的人数，currentRound表示当前是第几轮投票
        int quorum = Math.floorDiv(acceptors.size(), 2) + 1;
        int currentRound = 0;
        while (true) {
            printInfo("VOTE_ROUND", "START", ++currentRound + "");
            //当前Proposer获得的所有Proposal
            List<Proposal> proposals = new ArrayList<Proposal>();
            //遍历每一个Acceptor，查看其已被Accept的提案
            //prepare阶段，Proposer可以收到Acceptor有效回应的个数（具体指的是节点有响应，且当前提案大于Acceptor接受的最大提案）
            for (Acceptor acceptor : acceptors) {
                Promise promise = acceptor.onPrepare(proposal);
                if (promise != null && promise.isAck()){
                    proposals.add(promise.getProposal());
                }
            }
            //如果没有收到半数以上的节点的认可，则重新发起提案
            if (proposals.size() < quorum) {
                printInfo("PROPOSER[" + proposal + "]", "VOTE", "NOT PREPARED");
                proposal = nextProposal(proposal.getVoteNumber(), proposals);
                continue;
            }
            //提案被多少Acceptor接受
            int acceptCount = 0;
            for (Acceptor acceptor : acceptors) {
                if (acceptor.onAccept(proposal))
                    acceptCount++;
            }
            //当Proposer收到超过半数的回复时，说明自己的提案已经被接受。否则回到第一步重新发起提案。
            if (acceptCount < quorum) {
                printInfo("PROPOSER[" + proposal + "]", "VOTE", "NOT ACCEPTED");
                proposal = nextProposal(proposal.getVoteNumber(), proposals);
                continue;
            }
            break;
        }
        printInfo("PROPOSER[" + proposal + "]", "VOTE", "SUCCESS");
    }
}
