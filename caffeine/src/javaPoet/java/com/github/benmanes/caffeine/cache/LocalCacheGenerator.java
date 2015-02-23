/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.cache;

import static com.github.benmanes.caffeine.cache.Specifications.BOUNDED_LOCAL_CACHE;
import static com.github.benmanes.caffeine.cache.Specifications.BUILDER_PARAM;
import static com.github.benmanes.caffeine.cache.Specifications.CACHE_LOADER;
import static com.github.benmanes.caffeine.cache.Specifications.CACHE_LOADER_PARAM;
import static com.github.benmanes.caffeine.cache.Specifications.kRefQueueType;
import static com.github.benmanes.caffeine.cache.Specifications.kTypeVar;
import static com.github.benmanes.caffeine.cache.Specifications.vRefQueueType;
import static com.github.benmanes.caffeine.cache.Specifications.vTypeVar;

import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

import com.github.benmanes.caffeine.cache.Specifications.Strength;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * Generates a cache implementation.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class LocalCacheGenerator {
  private final Modifier[] privateFinalModifiers = { Modifier.PRIVATE, Modifier.FINAL };

  private final TypeSpec.Builder cache;
  private final Strength keyStrength;
  private final Strength valueStrength;
  private final boolean cacheLoader;

  LocalCacheGenerator(String className, Strength keyStrength, Strength valueStrength,
      boolean cacheLoader) {
    this.cache = TypeSpec.classBuilder(className);
    this.valueStrength = valueStrength;
    this.keyStrength = keyStrength;
    this.cacheLoader = cacheLoader;
  }

  public TypeSpec generate() {
    cache.addModifiers(Modifier.FINAL)
        .addTypeVariable(kTypeVar)
        .addTypeVariable(vTypeVar)
        .superclass(BOUNDED_LOCAL_CACHE);
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
        .addParameter(BUILDER_PARAM)
        .addParameter(CACHE_LOADER_PARAM)
        .addStatement("super(builder, cacheLoader, false)");

    addKeyStrength();
    addValueStrength();
    addCacheLoader(constructor);
    return cache.addMethod(constructor.build()).build();
  }

  private void addKeyStrength() {
    addStrength(keyStrength, "keyReferenceQueue", kRefQueueType);
  }

  private void addValueStrength() {
    addStrength(valueStrength, "valueReferenceQueue", vRefQueueType);
  }

  private void addCacheLoader(MethodSpec.Builder constructor) {
    if (!cacheLoader) {
      return;
    }
    constructor.addStatement("this.cacheLoader = cacheLoader");
    cache.addField(FieldSpec.builder(CACHE_LOADER, "cacheLoader", privateFinalModifiers).build());
    cache.addMethod(MethodSpec.methodBuilder("cacheLoader")
        .addStatement("return cacheLoader")
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .addAnnotation(Nullable.class)
        .returns(CACHE_LOADER)
        .build());
  }

  /** Adds a reference queue if needed, otherwise delegates to the default (no-op) method. */
  private void addStrength(Strength strength, String name, TypeName type) {
    if (strength == Strength.STRONG) {
      return;
    }
    MethodSpec.Builder method = MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PROTECTED)
        .addAnnotation(Override.class)
        .addAnnotation(Nullable.class)
        .returns(type);
    method.addStatement("return $N", name);
    FieldSpec field = FieldSpec.builder(type, name, privateFinalModifiers)
        .initializer("new $T()", type)
        .build();
    cache.addField(field);
    cache.addMethod(method.build());
  }
}
