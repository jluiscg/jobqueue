package com.lcortes.jobqueue.handler;

import com.fasterxml.jackson.databind.JsonNode;

public record JobResult(
        JsonNode output
) {}