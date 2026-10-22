# btcr2-dids-experiments

Datasets for analysis of `did:btcr2` performance, scalability, etc.

| Dataset                | DIDs | Update Rounds | Update Count |   Update probabilities    |                 Beacon type probabilities                 | Sidecar/CAS probabilities  |
|:-----------------------|:----:|:-------------:|:------------:|:-------------------------:|:---------------------------------------------------------:|:---------------------------|
| [data1](./data1)     |  25  |      10       |      60      | 0.25 (true), 0.75 (false) | 0.00 (SingletonBeacon) 0.25 (SMTBeacon), 0.75 (CASBeacon) | 1.00 (Sidecar), 0.00 (CAS) |
| [data2](./data2)     | 100  |      15       |     374      | 0.25 (true), 0.75 (false) | 0.00 (SingletonBeacon) 0.25 (SMTBeacon), 0.75 (CASBeacon) | 1.00 (Sidecar), 0.00 (CAS) |
| [data3](./data3)     | 100  |      20       |     403      | 0.20 (true), 0.80 (false) | 0.50 (SingletonBeacon) 0.25 (SMTBeacon), 0.25 (CASBeacon) | 1.00 (Sidecar), 0.00 (CAS) |
| [data3-1](./data3-1) | 100  |      20       |     403      | 0.20 (true), 0.80 (false) | 0.50 (SingletonBeacon) 0.25 (SMTBeacon), 0.25 (CASBeacon) | 0.90 (Sidecar), 0.10 (CAS) |
| [data3-2](./data3-2) | 100  |      20       |     403      | 0.20 (true), 0.80 (false) | 0.50 (SingletonBeacon) 0.25 (SMTBeacon), 0.25 (CASBeacon) | 0.75 (Sidecar), 0.25 (CAS) |
| [data3-3](./data3-3) | 100  |      20       |     403      | 0.20 (true), 0.80 (false) | 0.50 (SingletonBeacon) 0.25 (SMTBeacon), 0.25 (CASBeacon) | 0.50 (Sidecar), 0.50 (CAS) |
