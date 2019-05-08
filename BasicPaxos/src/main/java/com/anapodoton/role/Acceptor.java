package com.anapodoton.role;

import com.anapodoton.Promise;
import com.anapodoton.Proposal;
import static com.anapodoton.BasicPaxos.printInfo;

/**
 *
 */
public class Acceptor {
    //last保存之前最大的提案
    private Proposal last = new Proposal();

    private String name;

    public Acceptor(String name) {
        this.name = name;
    }

    /**
     * Proposer选择一个提案编号n，发送Prepare(n)请求给超过半数（或更多）的Acceptor。
     * Acceptor收到消息后，如果n比它之前见过的编号大，就回复这个消息，而且以后不会接受小于n的提案。另外，如果之前已经接受了小于n的提案，回复那个提案编号和内容给Proposer。
     * @param proposal 提案
     * @return Promise 承诺
     */
    public Promise onPrepare(Proposal proposal) {
        //假设这个过程有50%的几率失败，用随机数来模拟节点出现故障的情况
        if (Math.random() - 0.5 > 0) {
            printInfo("ACCEPTER_" + name, "PREPARE", "NO RESPONSE");
            return null;
        }
        //判断非法输入
        if (proposal == null)
            throw new IllegalArgumentException("null proposal");
        //如果提案编号n比它之前见过的编号大，就回复这个消息，而且以后不会接受小于n的提案；另外，如果之前已经接受了小于n的提案，回复那个提案编号和内容给Proposer。
        if (proposal.getVoteNumber() > last.getVoteNumber()) {
            Promise response = new Promise(true, last);
            last = proposal;
            printInfo("ACCEPTER_" + name, "PREPARE", "OK");
            return response;
        } else {//当前提案的编号过小
            printInfo("ACCEPTER_" + name, "PREPARE", "REJECTED");
            return new Promise(false, null);
        }
    }

    /**
     * Acceptor收到消息后，如果n大于等于之前见过的最大编号，就记录这个提案编号和内容，回复请求表示接受
     * @param proposal
     * @return
     */
    public boolean onAccept(Proposal proposal) {
        //假设这个过程有50%的几率失败
        if (Math.random() - 0.5 > 0) {
            printInfo("ACCEPTER_" + name, "ACCEPT", "NO RESPONSE");
            return false;
        }
        printInfo("ACCEPTER_" + name, "ACCEPT", "OK");
        return last.equals(proposal);
    }
}
