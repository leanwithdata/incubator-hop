/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.www;

import org.apache.hop.core.gui.Point;
import org.apache.hop.core.logging.HopLogStore;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.owasp.encoder.Encode;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
public class StartExecutionPipelineServletTest {
  private PipelineMap mockPipelineMap;

  private StartExecutionPipelineServlet startExecutionPipelineServlet;

  @Before
  public void setup() {
    mockPipelineMap = mock(PipelineMap.class);
    startExecutionPipelineServlet = new StartExecutionPipelineServlet(mockPipelineMap);
  }

  @Test
  @PrepareForTest({Encode.class})
  public void testStartExecutionPipelineServletEscapesHtmlWhenPipelineNotFound()
      throws ServletException, IOException {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);

    StringWriter out = new StringWriter();
    PrintWriter printWriter = new PrintWriter(out);

    PowerMockito.spy(Encode.class);
    when(mockHttpServletRequest.getContextPath())
        .thenReturn(StartExecutionPipelineServlet.CONTEXT_PATH);
    when(mockHttpServletRequest.getParameter(anyString()))
        .thenReturn(ServletTestUtils.BAD_STRING_TO_TEST);
    when(mockHttpServletResponse.getWriter()).thenReturn(printWriter);

    startExecutionPipelineServlet.doGet(mockHttpServletRequest, mockHttpServletResponse);
    assertFalse(ServletTestUtils.hasBadText(ServletTestUtils.getInsideOfTag("H1", out.toString())));

    PowerMockito.verifyStatic(atLeastOnce());
    Encode.forHtml(anyString());
  }

  @Test
  @PrepareForTest({Encode.class})
  public void testStartExecutionPipelineServletEscapesHtmlWhenPipelineFound()
      throws ServletException, IOException {
    HopLogStore.init();
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    HttpServletResponse mockHttpServletResponse = mock(HttpServletResponse.class);
    Pipeline mockPipeline = mock(Pipeline.class);
    PipelineMeta mockPipelineMeta = mock(PipelineMeta.class);
    ILogChannel mockChannelInterface = mock(ILogChannel.class);
    StringWriter out = new StringWriter();
    PrintWriter printWriter = new PrintWriter(out);

    PowerMockito.spy(Encode.class);
    when(mockHttpServletRequest.getContextPath())
        .thenReturn(StartExecutionPipelineServlet.CONTEXT_PATH);
    when(mockHttpServletRequest.getParameter(anyString()))
        .thenReturn(ServletTestUtils.BAD_STRING_TO_TEST);
    when(mockHttpServletResponse.getWriter()).thenReturn(printWriter);
    when(mockPipelineMap.getPipeline(any(HopServerObjectEntry.class))).thenReturn(mockPipeline);
    when(mockPipeline.getLogChannel()).thenReturn(mockChannelInterface);
    when(mockPipeline.isReadyToStart()).thenReturn(true);
    when(mockPipeline.getLogChannelId()).thenReturn("test");
    when(mockPipeline.getPipelineMeta()).thenReturn(mockPipelineMeta);
    when(mockPipelineMeta.getMaximum()).thenReturn(new Point(10, 10));

    startExecutionPipelineServlet.doGet(mockHttpServletRequest, mockHttpServletResponse);
    assertFalse(ServletTestUtils.hasBadText(ServletTestUtils.getInsideOfTag("H1", out.toString())));

    PowerMockito.verifyStatic(atLeastOnce());
    Encode.forHtml(anyString());
  }
}
