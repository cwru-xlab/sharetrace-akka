import polars as pl

from pymongo.collection import Collection


def get_runtime_results(
        collection: Collection,
        group_by_randoms=True
) -> pl.DataFrame:
    group_id = {'networkType': '$networkFactory.type'}
    flattened_id = {
        'datasetId': '$_id.datasetId',
        'networkId': '$_id.networkId',
        'networkType': '$_id.networkType',
    }
    if group_by_randoms:
        group_id.update({
            'ctRandomType': '$networkFactory.timeFactory.random.type',
            'stRandomType': '$scoreFactory.timeFactory.random.type',
            'svRandomType': '$scoreFactory.random.type'
        })
        flattened_id.update({
            'ctRandomType': '$_id.ctRandomType',
            'stRandomType': '$_id.stRandomType',
            'svRandomType': '$_id.svRandomType',
        })
    return pl.DataFrame(collection.aggregate([
        {
            '$group': {
                '_id': group_id,
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
        {'$set': flattened_id},
        {'$unset': ['_id', 'count', 'runtime']}
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
