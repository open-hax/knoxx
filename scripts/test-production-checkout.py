#!/usr/bin/env python3
"""A reusable Knoxx qualifier must never resolve source in its caller's repo."""
from pathlib import Path
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[1]

class ProductionCheckoutTest(unittest.TestCase):
    def test_all_qualification_jobs_checkout_knoxx(self):
        workflow = yaml.load((ROOT / '.github/workflows/deploy-production.yml').read_text(), Loader=yaml.BaseLoader)
        observed = []
        for name, job in workflow['jobs'].items():
            for step in job.get('steps', []):
                if step.get('uses', '').startswith('actions/checkout@'):
                    self.assertEqual('open-hax/knoxx', step['with'].get('repository'), name)
                    self.assertEqual('false', step['with'].get('persist-credentials'), name)
                    observed.append(name)
        self.assertCountEqual(['source', 'integration-e2e', 'mutation', 'qualified'], observed)

if __name__ == '__main__':
    unittest.main()
