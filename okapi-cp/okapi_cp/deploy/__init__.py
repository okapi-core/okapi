# Copyright The OkapiCore Authors
# SPDX-License-Identifier: Apache-2.0

"""Deployment flows."""

from abc import ABC, abstractmethod


class DeployFlow(ABC):
  """Base interface for deployment flows."""

  @abstractmethod
  def run(self) -> None:
    raise NotImplementedError
