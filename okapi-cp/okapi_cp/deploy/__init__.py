"""Deployment flows."""

from abc import ABC, abstractmethod


class DeployFlow(ABC):
  """Base interface for deployment flows."""

  @abstractmethod
  def run(self) -> None:
    raise NotImplementedError
