#!/usr/bin/env python
# -*- coding: utf-8 -*-

from Template import Template


class TemplateEngine:
    liteBase = None;
    liteService = None;
    def __init__(self,liteBase,liteService):
        self.liteBase = liteBase;
        self.liteService = liteService;
        
    def render(self, path, context, out):
        
