import polars as pl

from pymongo import MongoClient
from pymongo.collection import Collection


def get_runtime_results(collection: Collection) -> pl.DataFrame:
    return pl.DataFrame(collection.aggregate([
        {
            '$group': {
                '_id': {
                    'networkType': '$networkFactory.type',
                    'ctRandomType': '$networkFactory.timeFactory.random.type',
                    'stRandomType': '$scoreFactory.timeFactory.random.type',
                    'svRandomType': '$scoreFactory.random.type'
                },
                'count': {'$count': {}},
                'runtime': {'$push': '$results.runtime.MessagePassing'},
            }
        },
        {
            '$set': {
                'runtime': {'$slice': ['$runtime', {'$multiply': ['$count', -1]}]},
                'count': {'$subtract': ['$count', 1]}
            }
        },
        {
            '$set': {
                'avgRuntime': {'$avg': '$runtime'},
                'seRuntime': {'$divide': [{'$multiply': [1.96, {'$stdDevSamp': '$runtime'}]}, {'$sqrt': '$count'}]}
            }
        },
        {
            '$set': {
                'upperRuntimeCi': {'$add': ['$avgRuntime', '$seRuntime']},
                'lowerRuntimeCi': {'$subtract': ['$avgRuntime', '$seRuntime']},
            }
        },
        {
            '$set': {
                'datasetId': '$_id.datasetId',
                'networkId': '$_id.networkId',
                'networkType': '$_id.networkType',
                'ctRandomType': '$_id.ctRandomType',
                'stRandomType': '$_id.stRandomType',
                'svRandomType': '$_id.svRandomType',
            }
        },
        {
            '$unset': ['_id', 'count', 'runtime']
        }
    ]))


def all_confidence_intervals_overlap(df: pl.DataFrame) -> bool:
    for network_type in df.select(pl.col.networkType).unique().to_series():
        for row_i in _get_network_type_rows(df, network_type):
            for row_j in _get_network_type_rows(df, network_type):
                lower = max(row_i['lowerRuntimeCi'], row_j['lowerRuntimeCi'])
                upper = min(row_i['upperRuntimeCi'], row_j['upperRuntimeCi'])
                if lower > upper:
                    return False
    return True


def _get_network_type_rows(df: pl.DataFrame, network_type: str):
    return df.filter(pl.col.networkType == network_type).iter_rows(named=True)
