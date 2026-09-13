#!/usr/bin/env node
import {mkdirSync,existsSync,writeFileSync,readFileSync,cpSync,copyFileSync} from 'node:fs';
import {randomBytes} from 'node:crypto';
import {resolve} from 'node:path';
const [directory,environment,backendImage,frontendImage]=process.argv.slice(2);
if(!directory||!['stealth','yoga','testing','staging'].includes(environment)||!backendImage||!frontendImage)throw Error('Usage: prepare-environment.mjs DIRECTORY ENV BACKEND_IMAGE FRONTEND_IMAGE');
const target=resolve(directory);mkdirSync(target,{recursive:true,mode:0o700});
for(const sub of ['state/content','state/generated-contracts','state/workspace'])mkdirSync(`${target}/${sub}`,{recursive:true});
cpSync(new URL('../contracts',import.meta.url),`${target}/contracts`,{recursive:true});
copyFileSync(new URL('../compose.environment.yaml',import.meta.url),`${target}/compose.yaml`);
const values={KNOXX_BACKEND_IMAGE:backendImage,KNOXX_FRONTEND_IMAGE:frontendImage,KNOXX_PUBLIC_BASE_URL:`https://${environment}.knoxx.promethean.rest`,KNOXX_AXXIUM_ORIGIN:`https://${environment}.axxium.promethean.rest`,KNOXX_LISTEN_PORT:'18880'};
if(!existsSync(`${target}/.env`)){values.KNOXX_SESSION_SECRET=randomBytes(32).toString('hex');values.KNOXX_API_KEY=randomBytes(32).toString('hex');writeFileSync(`${target}/.env`,Object.entries(values).map(([k,v])=>`${k}=${v}`).join('\n')+'\n',{mode:0o600});}
else {let text=readFileSync(`${target}/.env`,'utf8');for(const key of ['KNOXX_BACKEND_IMAGE','KNOXX_FRONTEND_IMAGE'])text=text.replace(new RegExp(`^${key}=.*$`,'m'),`${key}=${values[key]}`);writeFileSync(`${target}/.env`,text,{mode:0o600});}
console.log(`Prepared isolated Knoxx ${environment} at ${target}; secrets remain local.`);
