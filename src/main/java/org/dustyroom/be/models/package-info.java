/**
 * Shared data models grouped by responsibility. Populate mutable DTOs before publishing
 * them to another thread; do not mutate a DTO while a worker and the UI share it.
 * Collection setters take defensive copies. Cache identity keys remain immutable.
 */
package org.dustyroom.be.models;
