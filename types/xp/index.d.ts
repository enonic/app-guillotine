import type {Content} from '@enonic-types/core';


export declare type MediaImageContent = Content<{
	altText?: string
	artist?: string[]
	caption?: string
	copyright?: string
	media: {
		attachment: string
		focalPoint: {
			x: number // Float
			y: number // Float
		}
	}
	tags?: string[]
}, 'media:image'>;

export declare type PortalSiteContent = Content<{
	description?: string
}, 'portal:site'>;
