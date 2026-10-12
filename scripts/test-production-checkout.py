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
        # Signing checks out the controller; qualified checks out controller and application.
        self.assertCountEqual(['source', 'integration-e2e', 'mutation',
                               'attest-evidence', 'qualified', 'qualified'], observed)

    def test_mutation_checkout_fetches_equivalence_basis_history(self):
        workflow = yaml.load((ROOT / '.github/workflows/deploy-production.yml').read_text(), Loader=yaml.BaseLoader)
        mutation = workflow['jobs']['mutation']
        checkouts = [step for step in mutation['steps']
                     if step.get('uses', '').startswith('actions/checkout@')]
        self.assertEqual(1, len(checkouts))
        self.assertEqual('${{ needs.source.outputs.sha }}', checkouts[0]['with']['ref'])
        self.assertEqual('0', checkouts[0]['with'].get('fetch-depth'),
                         'equivalence validation needs committed ancestor objects')
        self.assertEqual('false', checkouts[0]['with']['persist-credentials'])
        commands = '\n'.join(step.get('run', '') for step in mutation['steps'])
        self.assertIn('--equivalents mutation/reviewed-equivalents.edn', commands)

if __name__ == '__main__':
    unittest.main()
