package com.quantalabs.jamusync.util;

import com.quantalabs.jamusync.model.Transaction;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This class uses a Queue (FIFO - First In First Out) data structure to manage
 * pending guest orders. Orders are processed in the order they arrive, just like
 * a real queue of customers.
 *
 * The three classic Queue operations we use here are:
 *   - enqueue : add an order to the BACK of the queue   (addOrder)
 *   - dequeue : remove the order at the FRONT of the queue (processNextOrder)
 *   - peek    : look at the FRONT order without removing it (peekNextOrder)
 *
 * We use java.util.Queue with java.util.LinkedList, which is the classic Java
 * implementation of a Queue.
 */
public class PendingOrderQueue {

    // The Queue that holds all the pending orders.
    // LinkedList is the classic Java class used to build a Queue.
    private final Queue<Transaction> orders = new LinkedList<>();

    /**
     * enqueue: add a pending order to the BACK of the queue.
     * The newest order always goes to the end and will be served last.
     */
    public void addOrder(Transaction order) {
        orders.add(order); // enqueue - add to the back of the queue
    }

    /**
     * dequeue: remove and return the order at the FRONT of the queue.
     * This is the oldest waiting order, so it gets handled first (FIFO).
     * Returns null if the queue is empty.
     */
    public Transaction processNextOrder() {
        return orders.poll(); // dequeue - remove from the front of the queue
    }

    /**
     * peek: look at the order at the FRONT of the queue WITHOUT removing it.
     * Returns null if the queue is empty.
     */
    public Transaction peekNextOrder() {
        return orders.peek(); // peek - view the front of the queue
    }

    /**
     * Returns how many orders are currently waiting in the queue.
     */
    public int size() {
        return orders.size();
    }

    /**
     * Checks whether the queue has no orders waiting.
     */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /**
     * Returns all the orders in queue order (front to back) so they can be
     * shown on screen. The first item is the next one that will be processed.
     */
    public List<Transaction> getAllPending() {
        // Copy the queue into a normal list, keeping the FIFO order.
        return new LinkedList<>(orders);
    }
}
