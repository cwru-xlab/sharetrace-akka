from __future__ import annotations

import json
import os
from typing import AnyStr

from pydantic import BaseModel, BaseConfig, Field

from hints import Record


class SettingsModel(BaseModel):
    class Config(BaseConfig):
        allow_mutation = False


class UserParams(SettingsModel):
    trans_rate: float = Field(alias="transRate")
    send_coeff: float = Field(alias="sendCoeff")
    time_buffer: int = Field(alias="timeBuffer")
    score_ttl: int = Field(alias="scoreTtl")
    contact_ttl: int = Field(alias="contactTtl")
    tolerance: float = Field(alias="tolerance")
    timeout: int = Field(alias="idleTimeout")


class CacheParams(SettingsModel):
    intervals: int = Field(alias="numIntervals")
    look_ahead: int = Field(alias="numLookAhead")
    interval: int = Field(alias="interval")
    refresh_period: int = Field(alias="refreshPeriod")


class MiscParams(SettingsModel):
    seed: int = Field(alias="seed")
    graph_type: str = Field(alias="graphType")


class Settings(BaseModel):
    user: UserParams
    cache: CacheParams
    misc: MiscParams

    @classmethod
    def parse(cls, record: Record) -> Settings:
        return Settings(
            user=UserParams(**record.pop("userParams")),
            cache=CacheParams(**record.pop("cacheParams")),
            misc=MiscParams(**record))


def load(path: os.PathLike | AnyStr) -> list[Settings]:
    with open(path) as file:
        return [Settings.parse(rec["setting"]) for rec in map(json.loads, file)]
